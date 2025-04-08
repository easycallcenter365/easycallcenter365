package com.telerobot.fs.acd;

import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.*;
import com.telerobot.fs.wshandle.impl.InboundMonitorDataPull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * 话务队列处理类； 系统将为每个业务组产生一个Handler对象，该对象负责处理指定业务组编号的所有呼入请求，
 * 
 ****/
public class InboundGroupHandler {

    private final static Logger log = LoggerFactory.getLogger(InboundGroupHandler.class);

    /** 存放当前业务组的外呼电话 **/
	private ArrayBlockingQueue<CallHandler> inboundCallQueue = new ArrayBlockingQueue<>(600, false);

	private Semaphore semaphore = new Semaphore(0);

	public int getQueueSize(){
        return inboundCallQueue.size();
    }

    /** 业务组信息，这里使用学校的固话作为groupId  **/
	private String groupId = null;

	/** 业务组信息 **/
	public String getGroupId() {
		return groupId;
	}

	/** 根据groupId创建呼出电话处理对象  **/
	public InboundGroupHandler(String _groupId) {
		this.groupId = _groupId;
        activeCallHandler();
	}

	private boolean disposed = false;

    /**
     * 添加一个话务请求到话务队列中
     * @param callDetailInfo
     * @return
     */
	public boolean addCallToQueue(CallHandler callDetailInfo) {
        if (callDetailInfo == null) {
            return false;
        }
        try {
			if(!inboundCallQueue.contains(callDetailInfo)) {
				this.inboundCallQueue.put(callDetailInfo);
			}else{
				String errorTips = "严重错误，添加了重复的排队对象到队列中, addCallToQueue(CallHandler)";
				log.error("{} {}",  callDetailInfo.getTraceId(), errorTips);
			}
            semaphore.release();
        } catch (Throwable e) {
			log.error("{} addCallToQueue error: {} ", callDetailInfo.getTraceId(), e.toString());
        	return false;
        }
        return true;
    }

	/**
	 * 添加一个话务请求到话务队列中
	 * @param callDetailInfo
	 * @return
	 */
	public static boolean addCallToQueue(CallHandler callDetailInfo, String skillGroupId) {
		InboundGroupHandler groupHandler = InboundGroupHandlerList.getInstance().
				getCallHandlerBySkillGroupId(skillGroupId);
		assert null != groupHandler;
		return groupHandler.addCallToQueue(callDetailInfo);
	}



	private void assignCall() {
		log.info("assignCall thread started :" + this.groupId.toString());
		while (!disposed) {
			try {
                semaphore.acquire();
				// the benefits of using peek function, keep original data,
				// to get the exact number of call waiting in acd queue
				CallHandler call = this.inboundCallQueue.peek();
				if (call == null) {
					ThreadUtil.sleep(10);
					continue;
				}

				log.info("{} try to assign call", call.getTraceId());

				int printerCounter = 0;
				// get free agent and assign call
				boolean answered = call.getInboundDetail().getAnsweredTime() > 0L;
				while (!disposed && !call.getInboundDetail().getHangup() && !answered) {
					List<SessionEntity> agentList = AppContextProvider.getBean(SysService.class).getFreeUserList(groupId);
					int agentNum =  agentList.size();
				    if(agentNum > 0){

						SessionEntity agentFound = null;
						for (SessionEntity agent : agentList) {
							MessageHandlerEngine engine = MessageHandlerEngineList.
									getInstance().getMsgHandlerEngine(agent.getSessionId());
							if (engine != null) {
								SessionEntity session = engine.getSessionInfo();
								if (session != null && session.tryLock()) {
									log.info("{} An free agent is successfully obtained. opnum={}, extnum={}, lastHangupTime={}",
											call.getTraceId(), agent.getOpNum(), agent.getExtNum(), agent.getLastHangupTime()
									);
									log.info("{} Set the busy lock status of an agent. uerId={}, extNum={}",
											call.getTraceId(),
											agent.getOpNum(),
											agent.getExtNum()
									);
									AppContextProvider.getBean(SysService.class).setAgentStatusWithBusyLock(agent.getOpNum(), AgentStatus.busy.getIndex());
									agentFound = agent;
									break;
								}
							}
						}

                        // 转接到当前座席的分机
						if(null != agentFound) {
							call.tryToBridgeCall(agentFound);
							break;
						}else{
							ThreadUtil.sleep(10);
						}
                    } else {
                        if(printerCounter == 5000) {
							log.info("{} No free agent is available. Continue to wait...", call.getTraceId());
							log.info("{} Current counter of queued calls:{}", getGroupId(), inboundCallQueue.size());
							printerCounter = 0;
						}
						printerCounter += 10;
						ThreadUtil.sleep(10);
					}
				}
				log.info("{} remove call from acd queue, caller={}", call.getTraceId(), call.getInboundDetail().getCaller());
			    this.inboundCallQueue.poll();

			    if(call.getCallMonitorInfo() != null){
					InboundMonitorDataPull.remove(call.getCallMonitorInfo());
				}

				log.info("{} current calls count in acd queue:{}", getGroupId(), inboundCallQueue.size());
			} catch (Throwable ex) {
				log.error("assignCall thread exception occurred, details: {}, {} ", ex.toString(), CommonUtils.getStackTraceString(ex.getStackTrace()));
			}
		}
		log.info("assignCall thread stopped. groupId is ：" + this.groupId);
		disposed = true;
	}

	public synchronized void dispose() {
		if (!this.disposed) {
			disposed = true;
			for(CallHandler call : inboundCallQueue) { call = null; }
			this.inboundCallQueue.clear();
			this.inboundCallQueue = null;
		}
	}

    private void activeCallHandler() {
	    // 启动线程；
        new Thread(new Runnable() {
            @Override
            public void run() {
                assignCall();
            }
        }, "distributeCall").start();

		// 启动线程；
		new Thread(new Runnable() {
			@SneakyThrows
			@Override
			public void run() {
				sendAcdQueueInfoToGroup();
			}
		}, "sendAcdQueueToGroup").start();
    }

    private volatile boolean sendQueueEmptyInfo = true;

	/**
	 * 把当前业务组排队的实时人数发送给所有客户端
	 */
	private void sendAcdQueueInfoToGroup() {
		while (true) {
			try {
				int queueNumber = inboundCallQueue.size();
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("queue_number", queueNumber);
				MessageResponse msg = new MessageResponse(
						RespStatus.ACD_GROUP_QUEUE_NUMBER, "Real-time queue number.", jsonObject
				);

				if(queueNumber > 0) {
					sendQueueEmptyInfo = false;
					MessageHandlerEngineList.doBroadcast(msg, groupId);
				}else{
					if(!sendQueueEmptyInfo){
						sendQueueEmptyInfo = true;
						MessageHandlerEngineList.doBroadcast(msg, groupId);
					}
				}
			} catch (Throwable e) {
				log.error("sendAcdQueueInfoToGroup error: {} {}", e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
			}
			ThreadUtil.sleep(2000);
		}
	}

}

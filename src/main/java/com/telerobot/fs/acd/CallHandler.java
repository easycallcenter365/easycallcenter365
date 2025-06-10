package com.telerobot.fs.acd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.bo.ChanneState;
import com.telerobot.fs.entity.bo.ChannelFlag;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.dto.CallMonitorInfo;
import com.telerobot.fs.entity.po.CdrDetail;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.global.CdrPush;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.*;
import com.telerobot.fs.wshandle.*;
import com.telerobot.fs.wshandle.impl.CallApi;
import com.telerobot.fs.wshandle.impl.InboundMonitorDataPull;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 单个呼入电话的处理线程
 **/
public class CallHandler {

	private final static Logger log = LoggerFactory.getLogger(CallHandler.class);
	private static ConcurrentHashMap<String, CallHandler> callTaskList = new ConcurrentHashMap<>(500);
	/**
	 * 转接电话时，是否隐藏客户号码
	 */
	private static boolean hideInboundNumber = Boolean.parseBoolean(
			SystemConfig.getValue("hide-inbound-number", "true")
	);
	/**
	 * 转接通话时是否播放工号
	 */
	private static boolean playOpNum = Boolean.parseBoolean(
			SystemConfig.getValue("inbound-play-opnum", "true")
	);

	private static int acdPlayQueueNumInterval =
			Integer.parseInt(SystemConfig.getValue("acd_play_queue_num_interval", "25"));

	/**
	 *  呼入者相关信息
	 */
	private volatile InboundDetail inboundDetail;
	public InboundDetail getInboundDetail()
	{
		return inboundDetail;
	}

	private static ThreadPoolExecutor transferCallThreadPool;

	private static ThreadPoolExecutor fsMsgThreadPool;

	private volatile String uuid = "";
	private volatile String bleg = "";
	private static final String BLEGSTR = "bleg";

	public String getTraceId(){
		return  uuid;
	}

	/**
	 *  转接座席超时时间
	 */
	private static int transferAgentTimeOut = Integer.parseInt(
			SystemConfig.getValue("inbound-transfer-agent-timeout")
	);

	private volatile SessionEntity agentSessionEntity = null;

	private volatile myESLEventListener eslListener;

	private volatile long lastPlayNoFreeAgentTime = System.currentTimeMillis() - 21000;

	private volatile boolean keepMusicPlayed = false;

	/**
	 * 通话转接中
	 */
	private volatile boolean transferring = false;

	/**
	 * 通话转接失败
	 */
	private volatile boolean transferFailed = false;



	/**
	 *  当前esl连接池对象;
	 */
	protected volatile EslConnectionPool eslConnectionPool;

	static {
		int fsEslMsgThreadPoolSize = Integer.parseInt(AppContextProvider.getEnvConfig("app-config.fs-esl-msg-thread-pool-size", "50"));
		fsMsgThreadPool = ThreadPoolCreator.create(fsEslMsgThreadPoolSize, "fsMsgThreadPool", 12L, 5000);
		int transferCallThreadPoolSize = Integer.parseInt( AppContextProvider.getEnvConfig("app-config.inbound-call.transfer-call-thread-pool-size", "15"));
		transferCallThreadPool = ThreadPoolCreator.create(transferCallThreadPoolSize, "transferCall", 24, 2000);
		startMonitor();
	}

	private AtomicInteger bLegIndex = new AtomicInteger(0);

	/**
	 *  生成分机转接的Leg的uuid； 如果使用固定的uuid可能会导致出现重复的uuid；
	 *   [switch_core_session.c:2394 Duplicate UUID! 解决历史上出现的这个错误]
	 * @return
	 */
	private String generateBLegStr(){
		 int index = bLegIndex.incrementAndGet();
		 return String.format("%s-%s-%d", this.uuid, BLEGSTR, index);
	}

	private static boolean inboundCallMonitorEnabled =  Boolean.parseBoolean(
			SystemConfig.getValue("inbound_call_monitor_enabled", "false")
	);
	private CallMonitorInfo callMonitorInfo;
	public CallMonitorInfo getCallMonitorInfo(){
        return callMonitorInfo;
	}

	public CallHandler(InboundDetail inboundDetail) {
		log.info("{} init callHandler object ...", inboundDetail.getUuid());
		this.inboundDetail = inboundDetail;
		this.uuid = inboundDetail.getUuid();
        this.bleg = generateBLegStr();
		this.eslConnectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
		eslListener = new myESLEventListener();
		this.eslConnectionPool.getDefaultEslConn().addListener(this.uuid,  eslListener);
		if(detectCallActive()) {
			callTaskList.put(uuid, this);
		}
		log.info("{} init CallHandler finished.", inboundDetail.getUuid());

		if(inboundCallMonitorEnabled) {
			CallMonitorInfo callMonitorInfo = new CallMonitorInfo(
					this.bleg,
					this.uuid,
					"",
					inboundDetail.getCaller(),
					inboundDetail.getRemoteVideoPort() > 0 ? PhoneCallType.VIDEO_CALL : PhoneCallType.AUDIO_CALL,
					CallDirection.INBOUND,
					inboundDetail.getGroupId(),
					inboundDetail.getInboundTime()
			);
			this.callMonitorInfo = callMonitorInfo;
			InboundMonitorDataPull.add(callMonitorInfo);
		}
	}

	/**
	 *  呼入电话被重新入队列之后，重新初始化某些参数
	 */
	public void ReInit(){
		transferring = false;
		transferFailed = false;
        inboundDetail.setTransferTime(0L);
        this.bleg = generateBLegStr();
	}

	/**
	 *  发送挂机指令; 释放计数器; 关闭Esl连接;
	 */
	public void hangup() {
		EslConnectionUtil.sendExecuteCommand(
				"hangup",
				"mandatory-hangup",
				this.uuid,
				this.eslConnectionPool
		);
	}

	/**
	 *  开启监视线程; 如果一个通话1分钟还收到机器人语音，则强制结束它;
	 *  （很有可能是未收到挂机信号，强制结束是为了避免ThreadNum泄漏）
	 *
	 */
	private static  void startMonitor(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						monitorCall();
					}catch(Exception ex){
						log.error("monitorCall error: {}", ex.toString(), CommonUtils.getStackTraceString(ex.getStackTrace()));
					}
					ThreadUtil.sleep(10);
				}
			}
		}, "monitorCallTaskThread").start();
	}

	private static void monitorCall(){
		Iterator<Map.Entry<String, CallHandler>> it = callTaskList.entrySet().iterator();
		int processCount = 0;
		while (it.hasNext()){
			Map.Entry<String, CallHandler> entry = it.next();
			CallHandler task = entry.getValue();
			long currentTime = System.currentTimeMillis();
			long timePassed = currentTime - task.inboundDetail.getTransferTime() + 200;
			boolean transferExpired = (task.inboundDetail.getTransferTime() > 0L) && (timePassed > transferAgentTimeOut * 1000);
			boolean answered = task.inboundDetail.getAnsweredTime() > 0L;
			boolean hangup = task.inboundDetail.getHangup();
			// 通话未挂机且未被应答
			boolean notAnsweredAndNotHangup = !answered && !hangup;
			boolean callExtensionNoAnswer = task.transferring && transferExpired && notAnsweredAndNotHangup;
			boolean transferExtensionFailed =  task.transferFailed;
			if(transferExtensionFailed || callExtensionNoAnswer){
				log.warn("{} Put the transfer-failed call back to the queue:{}", task.uuid, JSON.toJSONString(task.inboundDetail));
				EslConnectionUtil.sendExecuteCommand(
						"hangup",
						"Call-Extension-TimeOut",
						 task.bleg,
						 task.eslConnectionPool
				);
				// ReInit 必须在hangup之后，否则老的通话无法挂机;
				task.ReInit();
				if(InboundGroupHandler.addCallToQueue(task, task.getInboundDetail().getGroupId())){
					log.info("{} Put the transfer-failed call back to the queue successfully.",  task.getInboundDetail().getUuid());
				}
				processCount++;
				continue;
			}

			long waitMills = (System.currentTimeMillis() - task.lastPlayNoFreeAgentTime);
			long interval  = acdPlayQueueNumInterval *  1000;
			if (notAnsweredAndNotHangup && waitMills >= interval && !task.transferring) {
				StringBuilder sbTips = new StringBuilder();
				String tips = getQueueNumTips(task);
                if(!StringUtils.isNullOrEmpty(tips)){
					sbTips.append("file_string://");
					sbTips.append(tips);
				}else{
                     sbTips.append("$${sounds_dir}/ivr/noFreeAgent.wav");
				}

				EslConnectionUtil.sendExecuteCommand(
						"playback",
						sbTips.toString(),
						task.uuid,
						task.eslConnectionPool
				);
				task.lastPlayNoFreeAgentTime = System.currentTimeMillis();
				continue;
			}

			if (notAnsweredAndNotHangup && !task.keepMusicPlayed) {
				EslConnectionUtil.sendExecuteCommand(
						"endless_playback",
						"$${sounds_dir}/ivr/keep.wav",
						task.uuid,
						task.eslConnectionPool
				);
				EslConnectionUtil.sendExecuteCommand(
						"playback",
						"$${sounds_dir}/ivr/pleaseWait.wav",
						 task.uuid,
					 	 task.eslConnectionPool
				);
				task.keepMusicPlayed = true;
			}
		}
		if(processCount != 0) {
			log.info("monitorThread, call session counter：" + processCount);
		}
	}

	/**
	 *  获取排队人数播报提示音
	 * @return
	 */
	private static String getQueueNumTips(CallHandler task){
		InboundGroupHandler groupHandler = InboundGroupHandlerList.getInstance()
				.getCallHandlerBySkillGroupId(task.inboundDetail.getGroupId() );
		int queueNum = groupHandler.getQueueSize();
		if(queueNum == 1 || queueNum == 0) {
			return "";
		}
		queueNum = queueNum - 1;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("$${sounds_dir}/ivr/queue-wait-num-tips-start.wav");
		stringBuilder.append(";");
		stringBuilder.append("$${sounds_dir}/ivr/digits/");
		stringBuilder.append(queueNum);
		stringBuilder.append(".wav");
		stringBuilder.append(";");
		stringBuilder.append("$${sounds_dir}/ivr/queue-wait-num-tips-end.wav");
		return CommonUtils.joinTtsFiles(
				task.uuid, stringBuilder.toString(), false, false
		);
	}

	private  void removeFsListener() {
		try {
			log.info("{} remove FreeSWITCH eslListener, uuid={}", uuid, this.uuid);
			callTaskList.remove(this.uuid);
			EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().removeListener(this.uuid);
			//需要移除可能存在的bleg，避免产生内存泄漏;
			EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().removeListener(bleg);
		}catch (Throwable ex) {
			log.error("{} error occurs while removeFsListener, details: {}  {}",
					uuid, ex.toString(), CommonUtils.getStackTraceString(ex.getStackTrace()));
		}
	}

	/**
	 * 为异常情况生成挂机事件数据;
	 * @return
	 */
	protected   Map<String, String> generateHangupEvent(String hangupClause){
		Map<String, String> eslHeaders = new HashMap<>(5);
		eslHeaders.put("Event-Name", EventNames.CHANNEL_HANGUP);
		eslHeaders.put("Callee", this.inboundDetail.getCallee());
		eslHeaders.put("Unique-ID", this.uuid);
		eslHeaders.put("Hangup-Cause", hangupClause);
		eslHeaders.put("Event-Date-Timestamp", String.valueOf(System.currentTimeMillis() * 1000L));
		return eslHeaders;
	}

	/**
	 * 检测通话是否挂机;
	 * @return 已挂机返回false，未挂机返回true
	 */
	private boolean detectCallActive(){
		EslMessage apiResponseMsg = EslConnectionUtil.sendSyncApiCommand(
				"uuid_exists",
				 uuid,
				 this.eslConnectionPool
		);
		if (apiResponseMsg != null && apiResponseMsg.getBodyLines().size() != 0) {
			String apiResponseText = apiResponseMsg.getBodyLines().get(0);
			if ("false".equalsIgnoreCase(apiResponseText)) {
				log.info("{} call session is hangup, stop process.", getTraceId());
				eslListener.processFsMsg(this.generateHangupEvent("hangup-before-robot-process"));
				return false;
			}
		} else {
			log.info("{} uuid_exists check error, can not get apiResponseMsg...", getTraceId());
		}
		return true;
	}

	/**
	 *  转接电话到座席;
	 * @param agent
	 */
	public void tryToBridgeCall(SessionEntity agent) {
		transferCallThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				log.info("{} Try to transfer the call to agent: {}", uuid, JSON.toJSONString(agent));
				agentSessionEntity = agent;
				inboundDetail.setOpnum(agentSessionEntity.getOpNum());
				inboundDetail.setExtnum(agentSessionEntity.getExtNum());
				AcdSqlQueue.addToSqlQueue(inboundDetail);

				MessageHandlerEngine engine = MessageHandlerEngineList.getInstance().getMsgHandlerEngine(agent.getSessionId());
				if(null != engine){
                    //发送弹屏消息
					engine.sendReplyToAgent(new MessageResponse(RespStatus.NEW_INBOUND_CALL, "new inbound call", inboundDetail));
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("status", AgentStatus.busy.getIndex());
					// 座席置忙
					engine.sendReplyToAgent(new MessageResponse(RespStatus.STATUS_CHANGED, "status: busy", jsonObject));
                    transferring = true;
					playOpNumOnTransferring();

					CallApi callApi = ((CallApi)engine.getMessageHandleByName("call"));
					if(null != callApi) {
						String displayNumber = hideInboundNumber ?
								CommonUtils.hiddenPhoneNumber(inboundDetail.getCaller()) : inboundDetail.getCaller();
						eslConnectionPool.getDefaultEslConn().addListener(bleg, eslListener);
						String callType = inboundDetail.getRemoteVideoPort() > 0 ? PhoneCallType.VIDEO_CALL : PhoneCallType.AUDIO_CALL;

					    SwitchChannel agentChannel = new SwitchChannel(bleg, uuid, callType, CallDirection.INBOUND);
						agentChannel.setPhoneNumber(callApi.getExtNum());

						SwitchChannel customerChannel = new SwitchChannel(uuid, bleg, callType, CallDirection.INBOUND);
						customerChannel.setPhoneNumber(inboundDetail.getCaller());
						customerChannel.setChannelState(ChanneState.BRIDGED);
						customerChannel.setFlag(ChannelFlag.HOLD_CALL);

						String asyncJobId = callApi.connectAgentExtNum(agentChannel, customerChannel,
								displayNumber, transferAgentTimeOut, inboundDetail.getInboundTime());

						eslConnectionPool.getDefaultEslConn().addListener(asyncJobId, eslListener);
						eslListener.setBackgroundJobUuid(asyncJobId);

						inboundDetail.setTransferTime(System.currentTimeMillis());
						log.info("{} transfer call response BackgroundJobUuid {}", bleg, asyncJobId);
					}else{
						log.error("{} cant not transfer the call, can not get callApi instance.", uuid);
					}
				}else{
					log.error("{} cant not get  MessageHandlerEngine instance, cant not send pop-window msg to agent = {}", uuid, agent.getOpNum());
				}
			}
		});
	}

	private void playOpNumOnTransferring(){
		if (playOpNum) {
			// 播放为当前通话服务的客服人员工号
			String destOpNum = agentSessionEntity.getOpNum();
			// 打断历史播放的等待音乐
			EslConnectionUtil.sendSyncApiCommand(
					"uuid_break" ,
					String.format("%s all", uuid),
					eslConnectionPool
			);
			ThreadUtil.sleep(50);
			for (int i = 0; i <= destOpNum.length() - 1; i++) {
				EslConnectionUtil.sendExecuteCommand(
						"playback",
						"$${sounds_dir}/ivr/digits/" + destOpNum.charAt(i) + ".wav",
						uuid,
						eslConnectionPool
				);
				ThreadUtil.sleep(400);
			}
			EslConnectionUtil.sendExecuteCommand(
					"playback",
					"$${sounds_dir}/ivr/servicesForYou.wav",
					uuid,
					eslConnectionPool
			);
			ThreadUtil.sleep(1200);
		}
	}

	/**
	 * 记录坐席挂机时间;
	 */
	private void saveAgentHangupTime(String extNum){
		try {
			// 记录坐席挂机时间;
			if (agentSessionEntity != null) {
				AppContextProvider.getBean(SysService.class).saveHangupTime(extNum);
			}
		}catch (Throwable e){
			log.error("{} saveAgentHangupTime error: {} {} ",
					getTraceId(), e.toString(), CommonUtils.getStackTraceString(e.getStackTrace())
			);
		}
	}

	/**
	 * 通话事件监听器 (用户应答、挂机、按键等事件时触发)
	 **/
	class myESLEventListener implements IEslEventListener {
		private final Logger log = LoggerFactory.getLogger(myESLEventListener.class);
		private String backgroundJobUuid = "";

		private myESLEventListener(){
		}

		public void setBackgroundJobUuid(String backgroundJobUuid) {
			this.backgroundJobUuid = backgroundJobUuid;
		}

		public void processFsMsg(Map<String, String> headers) {
			String uniqueID = headers.get("Unique-ID");
			String eventName = headers.get("Event-Name");
			String hangupCause = headers.get("Hangup-Cause")
					+ ":" + headers.get("variable_proto_specific_hangup_cause");
			// esl消息从产生到被处理的延迟时间; 毫秒数
			long eventTime = Long.parseLong(headers.get("Event-Date-Timestamp")) / 1000L;
			long now = System.currentTimeMillis();
			long timeDelay = now - eventTime;
			log.info("{} The [{}] event takes {} ms from generation to processing.", uuid, eventName, timeDelay);

			if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
				if (!uniqueID.contains(BLEGSTR)) {
					if(agentSessionEntity != null) {
						saveAgentHangupTime(agentSessionEntity.getExtNum());
					}
					log.info("{} save cdr,  hangupCause: {}", uuid, hangupCause);

					String transferToConferenceTime = headers.get("variable_transfer_to_conference_time");
					if(!StringUtils.isNullOrEmpty(transferToConferenceTime)){
						try {
							now = DateUtils.parseDateTime(URLDecoder.decode(transferToConferenceTime,"utf-8")).getTime();
							log.info("{} parse transferToConferenceTime successfully. {}", uuid, now);
						}catch(Throwable throwable) {
							log.error("{} parse transferToConferenceTime error! {} {}", uuid,
									throwable.toString(), CommonUtils.getStackTraceString(throwable.getStackTrace())
							);
						}
					}

					inboundDetail.setHangup(true);
					inboundDetail.setHangupTime(now);
					if(inboundDetail.getAnsweredTime() > 0L) {
						inboundDetail.setAnsweredTimeLen(now - inboundDetail.getAnsweredTime());
					}

					inboundDetail.setTimeLen(now - inboundDetail.getInboundTime());
					removeFsListener();
					AcdSqlQueue.addToSqlQueue(inboundDetail);

					// 推送话单;
					CdrDetail cdrDetail = new CdrDetail();
					cdrDetail.setUuid(uuid);
					cdrDetail.setCdrType("inbound");
					cdrDetail.setCdrBody(JSON.toJSONString(inboundDetail));
					CdrPush.addCdrToQueue(cdrDetail);
				}else{
					String extnum = headers.get("variable_extnum");
					saveAgentHangupTime(extnum);
					log.info("{} extension is hangup, hangupCause: {}", uuid, hangupCause);
					boolean answered = inboundDetail.getAnsweredTime() > 0L;
					if(!answered){
						log.error("{} The extension failed to answer the call. Extension {} is abnormal. Please pay attention to it!", uuid, extnum);
					}
				}

			} else if (EventNames.CHANNEL_PROGRESS_MEDIA.equalsIgnoreCase(eventName)) {
				log.info("{} recv ringing event {}", uuid, eventName);
			} else if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {
				if (uniqueID.contains(BLEGSTR)) {
					// 桥接通话
					log.info("{} the extension {} has been connected, try to bridge session. {}",
							uuid,
							headers.get("variable_extnum"),
							inboundDetail.getCaller()
					);

					EslMessage eslMessage = EslConnectionUtil.sendSyncApiCommand(
							"uuid_bridge",
							String.format("%s %s",uuid, uniqueID),
							eslConnectionPool
					);
					boolean bridgeSucceed = false;
					if(eslMessage.getBodyLines().size() > 0){
						if(eslMessage.getBodyLines().get(0).contains("+OK")){
							bridgeSucceed = true;
						}
					}
					if(!bridgeSucceed){
						log.error("{} call bridged failed： {}", uuid, JSON.toJSONString(eslMessage));
					}else{
						log.info("{} call bridged successfully： {}", uuid, JSON.toJSONString(eslMessage));
					}

					assert null != agentSessionEntity;
					// 记录当前接听者;
					inboundDetail.setOpnum(agentSessionEntity.getOpNum());
					inboundDetail.setExtnum(agentSessionEntity.getExtNum());
					inboundDetail.setAnsweredTime(System.currentTimeMillis());
					log.info("{} resetAgentBusyLockTime. uerId={}, extNum={}",
							uuid,
							agentSessionEntity.getOpNum(),
							agentSessionEntity.getExtNum()
					);
					AppContextProvider.getBean(SysService.class).resetAgentBusyLockTime(agentSessionEntity.getOpNum());
					AcdSqlQueue.addToSqlQueue(inboundDetail);
				}
			}
		}


		@Override
		public void eventReceived(String addr, EslEvent event) {
			// 这里创建单独的消息处理线程池， 避免拖慢Freeswitch消息处理速度
			fsMsgThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						processFsMsg(event.getEventHeaders());
					}catch (Exception e){
						log.error("{} processFsMsg error : {}  {}", uuid, e.toString(),
								CommonUtils.getStackTraceString(e.getStackTrace()));
					}
				}
			});
		}



		@Override
		public void backgroundJobResultReceived(String addr, EslEvent event) {
			eslConnectionPool.getDefaultEslConn().removeListener(this.backgroundJobUuid);
			String eslStr = event.toString();
			log.info("{} backgroundJobResultReceived [originate result] : {}" , uuid, eslStr);
			if (CommonUtils.checkTransferFailCase(eslStr)) {
				transferFailed = true;
				log.error("{} exception result got while connect extension，details：{}", uuid, event.toString());
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
			// 引用相等返回 true
		}
		// 如果等于 null，或者对象类型不同返回 false
		if (!(o instanceof CallHandler)) {
			return false;
		}
		// 强转为自定义 CallHandler 类型
		CallHandler callHandler = (CallHandler) o;
		// 如果uuid相等，就返回 true
		return uuid.equals(callHandler.uuid);
	}
	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}
}

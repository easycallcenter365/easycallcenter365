package com.telerobot.fs.outbound.notification;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.utils.*;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.CurrentTimeMillisClock;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 单个外呼任务线程 (执行外呼一个电话号码，并记录状态)
 **/
public class CallTask implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(CallTask.class);

	protected static ConcurrentHashMap<String, CallTask> callTaskList = new ConcurrentHashMap<>(500);

	/** 当前号码的相关信息 **/
	private volatile CallVoiceNotification phoneInfo = null;

	public CallVoiceNotification getPhoneInfo()
	{
		return phoneInfo;
	}

	/** 当前号码所属的任务批次信息 **/
	private volatile CallVoiceNotification batchEntity = null;

	public CallTask() {
	}

	private TaskManager batchMonitor = null;
	private static ThreadPoolExecutor fsMsgThreadPool = null;
	static {
		int fsEslMsgThreadPoolSize = Integer.parseInt(SystemConfig.getValue("voice-notification.fs-esl-msg-thread-pool-size", "10"));
		fsMsgThreadPool = ThreadPoolCreator.create(fsEslMsgThreadPoolSize, "fsMsgThreadPool", 12L, 5000);
		startMonitor();
	}

	/**
	 *  发送挂机指令; 释放计数器; 关闭Esl连接;
	 */
	public void hangup() {
		EslConnectionUtil.sendExecuteCommand(
				"hangup",
				"mandatory-hangup-by-monitorThread",
				this.callUuid,
				this.eslConnectionPool
		);
	}


	private static void doCheckCall(){
		int currentTasks = callTaskList.size();
		if(currentTasks != 0) {
			log.info("外呼模块monitorThread, 强行结束机器人异常通话线程，当前callTask数量是：" + currentTasks);
		}
		// 外呼机器人最长交互时长;
		Long maxSessionTime = Long.parseLong(SystemConfig.getValue("voice-notification.call_max_session_time", "601000"));
		Iterator<Map.Entry<String, CallTask>> it = callTaskList.entrySet().iterator();
		int processCount = 0;
		while (it.hasNext()){
			Map.Entry<String, CallTask> entry = it.next();
			CallTask task = entry.getValue();
			long currentTime = CurrentTimeMillisClock.now();
			long timePassed = currentTime - task.phoneInfo.getCalloutTime();
			boolean answered = task.phoneInfo.getConnectedTime() > 0L;
			boolean hangup = task.phoneInfo.getCallEndTime() > 0;
			long callTimeOut = CallConfig.callTimeOut * 1000;
			if(timePassed > callTimeOut && !answered && !hangup){
				task.endCall("呼叫超时未接通");
				task.phoneInfo.setCallstatus(3);
				processCount++;
				it.remove();
				task.eslListener.processFsMsg(task.generateHangupEvent("Exceed-callTimeOut"));
			}

			if(timePassed > maxSessionTime && answered && !hangup){
				task.endCall("机器人会话时长超过最大限制 batch_call_max_session_time");
				processCount++;
				it.remove();
				task.eslListener.processFsMsg(task.generateHangupEvent("Exceed-max_session_time"));
			}
		}
		if(processCount != 0) {
			log.info("外呼模块monitorThread, 强行结束机器人外呼通话数：" + processCount);
		}
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
					doCheckCall();
					ThreadUtil.sleep(11000);
				}
			}
		}, "monitorCallTaskThread").start();
	}

	private void endCall(String reason){
		log.info("{} 外呼模块强行结束外呼通话, 结束原因: {},  phoneNumEntity: {}" ,
				getTraceId(),
				reason,
				JSON.toJSONString(phoneInfo)
		);
		hangup();
		phoneInfo.setCallEndTime(System.currentTimeMillis());
	}

	/**
     *  外呼任务提交时间;
	 */
    private long taskSubmitTime = 0L;

	/**
	 * 创建一个外呼线程
	 *
	 * @param _phoneNumEntity
	 *            外呼号码信息
	 */
	public CallTask(TaskManager monitor, CallVoiceNotification _phoneNumEntity) {
		this.batchMonitor = monitor;
		this.phoneInfo = _phoneNumEntity;
        taskSubmitTime = System.currentTimeMillis();
	}

	/**
	 *  下载wav文件到本地
	 */
	private boolean downLoadWavFile(){
		String wavPath = phoneInfo.getVoiceFileSavePath();
		boolean exists = new File(wavPath).exists();
		if(StringUtils.isEmpty(wavPath) || !exists){
			String dir = String.format("%s%s/%s/",
						 SystemConfig.getValue("recording_path", "/home/Records/"),
						 "voice_files",
						 DateUtils.formatDate(new Date())
					);
			CommonUtils.safeCreateDirectory(dir);
			wavPath = String.format("%s%s.wav",
				      dir,
				      UuidGenerator.GetOneUuid()
				   );
		  boolean success = false;
		  try {
			  HttpDownloader.downloadFile(phoneInfo.getVoiceFileUrl(), wavPath);
			  success = true;
		  }catch (Throwable e){
		  	log.error("{} download wav file error: {} {}", phoneInfo.getTelephone(),
					e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
		  }
		  if(success &&  new File(wavPath).exists()){
             phoneInfo.setVoiceFileSavePath(wavPath);
		  }else{
		  	return false;
		  }
		}
       return  true;
	}

	@Override
	public void run() {
		try {
			doCall();
		} catch (Throwable e) {
			log.error("{} SingleCallTask 线程异常：{}", getTraceId(), e.toString());
			endCall("因程序异常发起主动挂机");
		}
	}


	private volatile boolean isReleased = false;
	private final Object lockerHelper = new Object();
	private  void releaseThreadNum() {
		if (!isReleased) {
			synchronized (lockerHelper) {
				if (!isReleased) {
					isReleased = true;
					try {
						batchMonitor.releaseThreadNum();
						callTaskList.remove(this.callUuid);
					} catch (Throwable ex) {
						log.error("{} 释放外线通道时发生了错误，详情: {}  {}" ,
								getTraceId() , ex.toString(), CommonUtils.getStackTraceString(ex.getStackTrace()));
					}
				}
			}
		}
	}

	/**
	 * 通话事件监听器 (用户应答、挂机、按键等事件时触发)
	 **/
	class myESLEventListener implements IEslEventListener {
		private final Logger log = LoggerFactory.getLogger(this.getClass());
		private String backgroundJobUuid = "";

		private myESLEventListener(String robotCallUuid){
		}

		public void setBackgroundJobUuid(String backgroundJobUuid) {
			this.backgroundJobUuid = backgroundJobUuid;
		}

		public void processFsMsg(Map<String, String> headers) {
			String uniqueID = headers.get("Unique-ID");
			String caller = headers.get("Caller-Caller-ID-Number");
			String dest = headers.get("Caller-Destination-Number");
			String eventName = headers.get("Event-Name");
			String hangupCause = headers.get("Hangup-Cause")
					+ ":" + headers.get("variable_proto_specific_hangup_cause");
			// esl消息从产生到被处理的延迟时间; 毫秒数
			long eventTime = Long.parseLong(headers.get("Event-Date-Timestamp")) / 1000L;
			long timeDelay = System.currentTimeMillis() - eventTime;
			log.info("{} 当前电话从 [{}] 到ESL消息被处理耗时: {} ms", getTraceId(), eventName, timeDelay);

			if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
				log.info("{} 通话结束, uuid: {}", getTraceId(), uniqueID);
				phoneInfo.setCallEndTime(System.currentTimeMillis());
				releaseThreadNum();

				log.info("{} 挂机原因: {}", getTraceId(), hangupCause);
				if (phoneInfo.getConnectedTime() > 0L) {
					long callDuring = eventTime - phoneInfo.getConnectedTime();
					phoneInfo.setValidTimeLen((int) (callDuring / 1000));
				}
				if (phoneInfo.getConnectedTime() > 0L) {
					long callDuring = eventTime - phoneInfo.getConnectedTime();
					phoneInfo.setTimeLen((int) (callDuring / 1000));
				}
				if (phoneInfo.getConnectedTime() <= 0L) {
					phoneInfo.setCallstatus(3);
				}

				phoneInfo.setCallEndTime(System.currentTimeMillis());
				phoneInfo.setHangupCause(hangupCause);
				SQLQueue.addToSQLQueueForUpdate(phoneInfo);
				log.info("{} 未转接到机器人的通话挂机处理，保存通话状态: phoneNumEntity={} ", getTraceId(), JSON.toJSONString(phoneInfo));
			} else if (EventNames.CHANNEL_PROGRESS_MEDIA.equalsIgnoreCase(eventName)) {
				log.info("{} 收到振铃事件 {}", getTraceId(), eventName);
			} else if(EventNames.CHANNEL_PARK.equalsIgnoreCase(eventName)){
				parkSemaphore.release();
			}else if(EventNames.PLAYBACK_STOP.equalsIgnoreCase(eventName)){
				hangup();
			}
			else if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {
				long waitParkSemStartTime = System.currentTimeMillis();
				// 确认收到 CHANNEL_PARK 事件后，在执行后面的逻辑；否则 park 逻辑会导致机器人playback无声音;
				try {
					parkSemaphore.tryAcquire(1200L, TimeUnit.MILLISECONDS);
				}catch (Throwable e){
				}
				log.info("{}  等待通道信号量耗时:{}.",
						getTraceId(),
						System.currentTimeMillis() - waitParkSemStartTime
				);

				// 先设置接通状态
				long now = System.currentTimeMillis();
				phoneInfo.setConnectedTime(now);
				log.info(getTraceId() + " 电话已经接通, caller: {}, dest: {}, uuid: {}", caller, dest, uniqueID);
				phoneInfo.setCallstatus(4);

				String wavFile = String.format("/%s/%s/%s.wav",
						"voice-notification",
						DateUtils.format(new Date(now), "yyyy/MM/dd/HH"),
						callUuid
				);
				phoneInfo.setRecordingsFile(wavFile);
				String fullRecordPath = String.format("%s%s",
						SystemConfig.getValue("recording_path", "/home/Records/") ,
						wavFile
				);
				log.info("{} 开启通话全程录音: {}", getTraceId(), fullRecordPath);
				EslConnectionUtil.sendExecuteCommand("record_session", fullRecordPath, uniqueID);

				// playback wav file:
				EslConnectionUtil.sendExecuteCommand("playback", phoneInfo.getVoiceFileSavePath(), uniqueID);
				SQLQueue.addToSQLQueueForUpdate(phoneInfo);
			}
		}

		private Semaphore parkSemaphore = new Semaphore(0);

		@Override
		public void eventReceived(String addr, EslEvent event) {
			// 这里创建单独的消息处理线程池， 避免拖慢Freeswitch消息处理速度
			fsMsgThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						processFsMsg(event.getEventHeaders());
					}catch (Exception e){
						log.error("{} 处理 Freeswitch esl消息时出错 error : {}  {}, msg: {}",
								getTraceId(),
								e.toString(),
								CommonUtils.getStackTraceString(e.getStackTrace()),
								event.toString()
						);
					}
				}
			});
		}


		@Override
		public void backgroundJobResultReceived(String addr, EslEvent event) {
			eslConnectionPool.getDefaultEslConn().removeListener(this.backgroundJobUuid);
			String eslStr = event.toString();
			log.info("{} backgroundJobResultReceived 收到外呼结果 : {}" , getTraceId(), eslStr);
			if (!eslStr.contains("OK")) {
				log.info("{} single call failed, 外呼失败，详情：{}", getTraceId(), event.toString());
				releaseThreadNum();
			}
		}
	};


	private myESLEventListener eslListener=null;;
	private volatile String callUuid = "";
	private String getTraceId(){
		return callUuid;
	}
	protected EslConnectionPool eslConnectionPool;

	/**
	 *  获取esl连接池
	 * @param uuid
	 */
	protected void getEslConnectionPool(String uuid){
		EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
		if(connectionPool == null){
			log.error("{} 无法获取到esl连接池.", uuid);
			return;
		}
		this.eslConnectionPool = connectionPool;
	}

	/**
	 * 为异常情况生成挂机事件数据;
	 * @return
	 */
	protected   Map<String, String> generateHangupEvent(String hangupClause){
		Map<String, String> eslHeaders = new HashMap<>(5);
		eslHeaders.put("Event-Name", EventNames.CHANNEL_HANGUP);
		eslHeaders.put("Callee", this.phoneInfo.getTelephone());
		eslHeaders.put("Unique-ID", this.callUuid);
		eslHeaders.put("Hangup-Cause", hangupClause);
		eslHeaders.put("Event-Date-Timestamp", String.valueOf(CurrentTimeMillisClock.now() * 1000L));
		return eslHeaders;
	}

	/**
	 * 执行外呼
	 **/
	private void doCall() {
		boolean success = downLoadWavFile();
		if(!success){
			log.error("downLoadWavFile error, exit call process. {}", phoneInfo.getTelephone());
			phoneInfo.setCallstatus(5);
 			SQLQueue.addToSQLQueueForUpdate(phoneInfo);
			releaseThreadNum();
			return;
		}
		phoneInfo.setCallcount(phoneInfo.getCallcount() + 1);
        long startTime = System.currentTimeMillis();
        long timeSpent = startTime - taskSubmitTime;
        log.info("{} 本次外呼任务从提交到执行耗时 {}", this.callUuid, timeSpent);

		this.callUuid = UuidGenerator.GetOneUuid();

		log.info("{} Try send call request to freeSwitch Server {} ...",  getTraceId(),  this.phoneInfo.getTelephone());
		log.info("{} 内部外呼统计信息, 本批次已用外线数:{} , 总的外线已使用: {}",
				getTraceId() , batchMonitor.getThreadNumUsed(), CallConfig.getMaxLineNumber_Used());
        CurrentTimeMillisClock.now();
		getEslConnectionPool(this.callUuid);
		phoneInfo.setUuid(this.callUuid);

		callTaskList.put(this.callUuid, this);
		StringBuilder callPrefix = new StringBuilder();
		callPrefix.append(String.format(
				"hangup_after_bridge=true,origination_uuid=%s,absolute_codec_string=pcma,ignore_early_media=false,",
				callUuid
		));
		callPrefix.append(
				"record_waste_resources=true,record_sample_rate=8000,RECORD_STEREO=false,auto_batchcall_flag=1,"
		);

		String callee = StringUtils.trim(this.phoneInfo.getTelephone());
		String callParameter = String.format("{%s}sofia/gateway/MRWG/%s  &park()",
				callPrefix.toString(),
				callee
		);

		phoneInfo.setCalloutTime(System.currentTimeMillis());
		log.info("{}:拨打状态[{}]",callUuid, 2);
		phoneInfo.setCallstatus(2);
		SQLQueue.addToSQLQueueForUpdate(phoneInfo);

		eslListener = new myESLEventListener(callUuid);
		this.eslConnectionPool.getDefaultEslConn().addListener(this.callUuid,  eslListener);
		log.info("{} Call Parameters: originate {}", getTraceId(),  callParameter);
		String response = EslConnectionUtil.sendAsyncApiCommand("originate", callParameter, this.eslConnectionPool);
		log.info("{} fs bgapi originate 响应: {}", getTraceId(),  response);
		if(!StringUtils.isEmpty(response)){
			String jobId = response.trim();
			if(jobId.length() == 36) {
				this.eslConnectionPool.getDefaultEslConn().addListener(jobId, eslListener);
				eslListener.setBackgroundJobUuid(jobId);
				log.info("{} 已经为Job-ID= {} 订阅后续事件:", getTraceId(),  response);
			}else{
				log.error("{} fs bgapi originate 响应的uuid文本长度不是36位: {}", getTraceId(),  response);
			}
		}
        timeSpent = System.currentTimeMillis() -  startTime;
        log.info("{} 本次外呼任务执行耗时 {}", this.callUuid, timeSpent);
	}
}

package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.bo.ChanneState;
import com.telerobot.fs.entity.bo.ChannelFlag;
import com.telerobot.fs.entity.bo.ConferenceCommand;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.*;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class CallListener implements IEslEventListener {
	private static final Logger logger = LoggerFactory.getLogger(CallListener.class);
	private CallApi callApiObject = null;
	private String traceId = "";
	private String backgroundJobUuid = "";

	/**
	 * 坐席channel
	 */
	private SwitchChannel agentChannel;

	/**
	 *  客户channel
	 */
	private SwitchChannel customerChannel;

	private Semaphore callSemaphore = new Semaphore(0);

	/**
	 *  等待信号量
	 */
	public void waitForSignal(){
		try {
			callSemaphore.acquire();
		} catch (InterruptedException e) {
		}
	}

	/**
	 *  释放信号量
	 */
	public void releaseSignal() {
		callSemaphore.release();
	}


	public SwitchChannel getCustomerChannel() {
		return customerChannel;
	}

	public SwitchChannel getAgentChannel() {
		return agentChannel;
	}


	private String getTraceId() {
		if (StringUtils.isNullOrEmpty(traceId)) {
			traceId = callApiObject.getTraceId() + " " + agentChannel.getUuid() + ":";
		}
		return traceId;
	}

	public void setBackgroundJobUuid(String backgroundJobUuid) {
		this.backgroundJobUuid = backgroundJobUuid;
	}

	public CallListener(CallApi outerObj, SwitchChannel agentChannel, SwitchChannel customerChannel) {
		this.callApiObject = outerObj;
		this.agentChannel = agentChannel;
		this.customerChannel = customerChannel;
	}

	@Override
	public void eventReceived(String addr, EslEvent event) {
		Map<String, String> headers = event.getEventHeaders();
		String uniqueId = headers.get("Unique-ID");
		String eventName = headers.get("Event-Name");

		if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {

			logger.info("{} recv CHANNEL_ANSWER event.  uniqueId={}", getTraceId(), uniqueId);
			if (uniqueId.equalsIgnoreCase(customerChannel.getUuid())) {
				customerChannel.setAnsweredTime(System.currentTimeMillis());
				customerChannel.setChannelState(ChanneState.ANSWERED);

				//send callMonitor info
				if(customerChannel.getCallMonitorInfo() != null) {
					customerChannel.getCallMonitorInfo().setConnectedTimeStamp(System.currentTimeMillis());
					CallMonitorDataPull.add(customerChannel.getCallMonitorInfo());
				}

				if(customerChannel.getSendChannelStatusToWsClient()){
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("uuid", agentChannel.getUuid());
					if(customerChannel.getCallDirection().equalsIgnoreCase(CallDirection.OUTBOUND)){
						jsonObject.put("callType", customerChannel.getCallType());
					}
					logger.info("{} customerChannel is connected，call confirmed.", getTraceId());
					callApiObject.sendReplyToAgent(
							new MessageResponse(RespStatus.CALLEE_ANSWERED, "call connected.", jsonObject)
					);
				}

				if(customerChannel.getAnsweredHook() != null) {
					customerChannel.getAnsweredHook().onAnswered(headers, getTraceId());
					customerChannel.setAnsweredHook(null);
				}
			}else if (uniqueId.equalsIgnoreCase(agentChannel.getUuid())) {
				agentChannel.setAnsweredTime(System.currentTimeMillis());
				agentChannel.setChannelState(ChanneState.ANSWERED);

				if(agentChannel.getSendChannelStatusToWsClient()){
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("uuid", agentChannel.getUuid());
					if(agentChannel.getCallDirection().equalsIgnoreCase(CallDirection.INBOUND)){
						jsonObject.put("callType", agentChannel.getCallType());
					}
					if(customerChannel.getAnsweredTime() > 0L){
						// audio call convert to video call scenario.
						jsonObject.put("callType", agentChannel.getCallType());
					}
					logger.info("{} customerChannel is connected，call confirmed.", getTraceId());
					callApiObject.sendReplyToAgent(
							new MessageResponse(RespStatus.CALLER_ANSWERED, "call connected.", jsonObject)
					);
				}

				if(agentChannel.getCallDirection().equalsIgnoreCase(CallDirection.INBOUND)) {
					customerChannel.setAnsweredTime(System.currentTimeMillis());
					customerChannel.setChannelState(ChanneState.ANSWERED);

					if(agentChannel.testFlag(ChannelFlag.TRANSFER_CALL_RECV)){
						// remove exists callMonitorInfo
						if(agentChannel.getCallMonitorInfo() != null) {
							CallMonitorDataPull.remove(agentChannel.getCallMonitorInfo());
						}
					}

					//send callMonitor info
					if(agentChannel.getCallMonitorEnabled()) {
						agentChannel.getCallMonitorInfo().setConnectedTimeStamp(System.currentTimeMillis());
						if(agentChannel.getCallMonitorInfo() != null) {
							CallMonitorDataPull.add(agentChannel.getCallMonitorInfo());
						}
					}
				}

				if(agentChannel.getAnsweredHook() != null) {
					agentChannel.getAnsweredHook().onAnswered(headers, getTraceId());
					agentChannel.setAnsweredHook(null);
				}
			}
			releaseSignal();

		} else if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {

			logger.info("{} recv CHANNEL_HANGUP event.  uniqueId={}", getTraceId(), uniqueId);
			if (uniqueId.equalsIgnoreCase(customerChannel.getUuid())) {
				String hangupSipCode = headers.get("variable_sip_invite_failure_status");
				customerChannel.setHangupSipCode(hangupSipCode);
				customerChannel.setHangupTime(System.currentTimeMillis());
				customerChannel.setChannelState(ChanneState.HANGUP);

				if(customerChannel.getAnsweredTime() > 0L){
					logger.info("{} recv customerChannel hangup event，" +
							"due to customerChannel has been answered，" +
							"now we need not to keep call，so now we hangup extension.", getTraceId());
					endCall("customerChannel hangup.");
				}

				//send callMonitor info
				if(customerChannel.getCallMonitorInfo() != null) {
					CallMonitorDataPull.remove(customerChannel.getCallMonitorInfo());
				}

				if(customerChannel.getHangupHook() != null) {
					customerChannel.getHangupHook().onHangup(headers, getTraceId());
					customerChannel.setHangupHook(null);
				}
			}else if (uniqueId.equalsIgnoreCase(agentChannel.getUuid())) {
				agentChannel.setHangupTime(System.currentTimeMillis());
				agentChannel.setChannelState(ChanneState.HANGUP);

				//send callMonitor info
				if(agentChannel.getCallMonitorInfo() != null) {
					CallMonitorDataPull.remove(agentChannel.getCallMonitorInfo());
				}

				if(!agentChannel.testFlag(ChannelFlag.RE_INVITE_VIDEO)) {
					if(agentChannel.getSendChannelStatusToWsClient()) {
						JSONObject jsonObject = new JSONObject();
						String hangupSipCode = headers.get("variable_sip_invite_failure_status");
						String hangupCause = headers.get("Hangup-Cause");
						jsonObject.put("My-Hangup-Cause", hangupCause + ":" + hangupSipCode);
						callApiObject.sendReplyToAgent(
								new MessageResponse(RespStatus.CALLER_HANGUP, "extension is hangup.", jsonObject)
						);
					}

					if(!customerChannel.testFlag(ChannelFlag.HOLD_CALL)) {
						logger.info("{} extension is hangup，so we must kill customerChannel.", getTraceId());
						EslConnectionUtil.sendExecuteCommand(
								"hangup",
								"extension_hangup",
								customerChannel.getUuid()
						);
					}
				}

				if(agentChannel.getHangupHook() != null) {
					agentChannel.getHangupHook().onHangup(headers, getTraceId());
					agentChannel.setHangupHook(null);
				}
			}
			releaseSignal();

		} else if (EventNames.CHANNEL_PROGRESS_MEDIA.equalsIgnoreCase(eventName)) {

			logger.info("{} recv CHANNEL_PROGRESS_MEDIA event.  uniqueId={}", getTraceId(), uniqueId);
			if (uniqueId.equalsIgnoreCase(customerChannel.getUuid())) {
				customerChannel.setFlag(ChannelFlag.RECV_RING_MEDIA);

				if(customerChannel.getRecvMediaHook() != null) {
					customerChannel.getRecvMediaHook().onRecvMedia(headers, getTraceId());
					customerChannel.setRecvMediaHook(null);
				}
			}else if (uniqueId.equalsIgnoreCase(agentChannel.getUuid())) {
				if(agentChannel.getRecvMediaHook() != null) {
					agentChannel.getRecvMediaHook().onRecvMedia(headers, getTraceId());
					agentChannel.setRecvMediaHook(null);
				}
			}

		} else if (EventNames.RECORD_START.equalsIgnoreCase(eventName)) {
			logger.info("{} recv record_start confirmed. ", getTraceId());

		} else if (EventNames.RECORD_STOP.equalsIgnoreCase(eventName)) {
			logger.info("{} recv record_stop confirmed. ", getTraceId());

		}else if (EventNames.CHANNEL_PARK.equalsIgnoreCase(eventName)) {
			logger.info("{} recv CHANNEL_PARK event.  uniqueId={}", getTraceId(), uniqueId);
			if (uniqueId.equalsIgnoreCase(customerChannel.getUuid())) {
				if(customerChannel.getBridgeCallAfterPark()){
					customerChannel.setBridgeCallAfterPark(false);
					customerChannel.setChannelState(ChanneState.PARKED);
					agentChannel.setChannelState(ChanneState.PARKED);
					logger.info("{} onPark event occurred, try to bridge call ... ", getTraceId());
					callApiObject.bridgeCall(agentChannel, customerChannel);
				}

                if(customerChannel.getParkHook() != null) {
					customerChannel.getParkHook().onPark(headers, getTraceId());
					customerChannel.setParkHook(null);
                }
			}else if (uniqueId.equalsIgnoreCase(agentChannel.getUuid())) {
				if(agentChannel.getBridgeCallAfterPark()){
					agentChannel.setBridgeCallAfterPark(false);
					customerChannel.setChannelState(ChanneState.PARKED);
					agentChannel.setChannelState(ChanneState.PARKED);
					logger.info("{} onPark event occurred, try to bridge call ... ", getTraceId());
					callApiObject.bridgeCall(agentChannel, customerChannel);
				}

				if(agentChannel.getParkHook() != null) {
					agentChannel.getParkHook().onPark(headers, getTraceId());
					agentChannel.setParkHook(null);
				}
			}
		}
	}

	@Override
	public void backgroundJobResultReceived(String addr, EslEvent event) {
		EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().removeListener(this.backgroundJobUuid);
		String apiResponse = event.toString();
		logger.info("recv BACKGROUND_JOB : {}" , apiResponse);
		Map<String, String> headers = event.getEventHeaders();

		boolean responseOK = apiResponse.contains("OK");
		if(agentChannel.getCallDirection().equalsIgnoreCase(CallDirection.OUTBOUND)) {
			boolean callFailed = customerChannel.getAnsweredTime() == 0L &&
					!customerChannel.testFlag(ChannelFlag.RECV_RING_MEDIA);
			if (!responseOK && callFailed) {
				releaseSignal();
				ProcessDialedResultCase(apiResponse);
			}
		}else{
			if (!responseOK) {
				releaseSignal();
				ProcessDialedResultCase(apiResponse);
			}
		}

		if(agentChannel.getRecvBgApiResultHook() != null) {
			agentChannel.getRecvBgApiResultHook().onRecv(event.toString(), getTraceId());
			agentChannel.setRecvBgApiResultHook(null);
		}

		if(customerChannel.getRecvBgApiResultHook() != null) {
			customerChannel.getRecvBgApiResultHook().onRecv(event.toString(), getTraceId());
			customerChannel.setRecvBgApiResultHook(null);
		}
	}

	public void clearCustomerHoldCallFlag(){
		if(customerChannel.testFlag(ChannelFlag.HOLD_CALL)) {
			customerChannel.clearFlag(ChannelFlag.HOLD_CALL);
		}
	}

	/**
	 *  判断通话是否正常结束了；
	 *  判断条件如下：
	 *  收到了振铃提示语音； 或者
	 *  客户已经应答； 或者
	 *  外呼结束时返回的sipCode不是 480/404/500等；
	 * @return
	 */
	public boolean checkCustomerChannelCallStatus(){
		String hangupSipCode = customerChannel.getHangupSipCode();
		if(hangupSipCode.startsWith("4") || hangupSipCode.startsWith("5")){
			return false;
		}
		if(customerChannel.testFlag(ChannelFlag.RECV_RING_MEDIA)){
			return true;
		}
		if(customerChannel.getAnsweredTime() > 0L){
			return true;
		}
		return false;
	}

	/**
	 * reset customerChannel last call status data
	 */
	public void resetCustomerChannelLastCallStatus(){
		customerChannel.setHangupSipCode("");
		customerChannel.clearFlag(ChannelFlag.RECV_RING_MEDIA);
		customerChannel.setAnsweredTime(0L);
		customerChannel.setHangupTime(0L);
	}


	/**
	 * 结束通话
	 */
	public synchronized void endCall(String... hangupReason) {
		if (agentChannel.getHangupTime() == 0L) {
			logger.info("{} recv hangup extension command", getTraceId());
			String reason = "manually_hangup";
			if(hangupReason.length > 0){
				reason = hangupReason[0];
			}
			EslConnectionUtil.sendExecuteCommand(
					"hangup",
					reason,
					agentChannel.getUuid()
			);
		}
	}

	public synchronized void playMp4File(String mp4FilePath){
		if(PhoneCallType.AUDIO_CALL.equalsIgnoreCase(agentChannel.getCallType())){
			callApiObject.sendReplyToAgent(
					new MessageResponse(RespStatus.REQUEST_PARAM_ERROR,
							"Current call is audio-call. Cant not send video file.")
			);
			return;
		}

		if (customerChannel.getHangupTime() > 0L) {
			callApiObject.sendReplyToAgent(
					new MessageResponse(RespStatus.REQUEST_PARAM_ERROR,
							"customerChannel is hangup already.")
			);
			return;
		}

		EslConnectionUtil.sendExecuteCommand(
				"set",
				"park_after_bridge=true",
				customerChannel.getUuid()
		);
		ThreadUtil.sleep(20);
		customerChannel.setFlag(ChannelFlag.HOLD_CALL);

		if (agentChannel.getHangupTime() == 0L) {
			// 先挂断内线分机
			EslConnectionUtil.sendExecuteCommand(
					"hangup",
					"",
					agentChannel.getUuid()
			);
		}

		// 必须等待挂机消息确认后，才可以发送视频文件给customerChannel;
		// 否则会导致播放异常;
		int counter = 0;
		while (agentChannel.getHangupTime() == 0L && counter < 200){
			ThreadUtil.sleep(5);
			counter ++;
		}

		logger.info("{} try to playMp4File={} for user={}",
				getTraceId(), mp4FilePath, customerChannel.getPhoneNumber()
		);

		CallIVR.playMp4File(customerChannel.getPhoneNumber(),
				customerChannel.getUuid(),
				mp4FilePath,
				3
		);

	}

	public synchronized void reInviteVideo(SessionEntity sessionEntity){
		if(PhoneCallType.VIDEO_CALL.equalsIgnoreCase(agentChannel.getCallType())){
			callApiObject.sendReplyToAgent(
					new MessageResponse(RespStatus.REQUEST_PARAM_ERROR,
							"Current call is video-call already.")
			);
			return;
		}
		if (agentChannel.getHangupTime() == 0L) {
			if(agentChannel.getCallDirection().equalsIgnoreCase(CallDirection.INBOUND)){
				EslConnectionUtil.sendExecuteCommand(
						"set",
						"park_after_bridge=true",
						customerChannel.getUuid()
				);
				ThreadUtil.sleep(20);
			}

			// 发送视频邀请;
			String param = String.format("%s = pcma,h264", customerChannel.getUuid());
			EslConnectionUtil.sendAsyncApiCommand("uuid_media_reneg", param);

			long startTime = 0L;
			logger.info("{} hangup extension {}, UuidInner={}, before reInviteVideo",
					getTraceId(), sessionEntity.getExtNum() , agentChannel.getUuid() );

			agentChannel.setFlag(ChannelFlag.RE_INVITE_VIDEO);
			// 先挂断内线分机
			EslConnectionUtil.sendExecuteCommand(
					"hangup",
					"RE_INVITE_VIDEO",
					agentChannel.getUuid()
			);

			startTime = System.currentTimeMillis();
			int counter = 0;
			while (agentChannel.getHangupTime() == 0L && counter < 200){
				ThreadUtil.sleep(5);
				counter ++;
			}
			logger.info("{} reInviteVideo , Wait for extension hangup , cost {} mills.", getTraceId(), System.currentTimeMillis() - startTime);
			ThreadUtil.sleep(100);
			agentChannel.setAnsweredTime(0L);
			agentChannel.setHangupTime(0L);
			agentChannel.setCallType(PhoneCallType.VIDEO_CALL);
			agentChannel.setBridgeCallAfterPark(true);
			customerChannel.setCallType(PhoneCallType.VIDEO_CALL);

			agentChannel.clearFlag(ChannelFlag.RE_INVITE_VIDEO);
			// 重新接通分机
			callApiObject.connectExtension(agentChannel, customerChannel);
		}
	}


	/***
	 * 处理各种呼叫失败的情况
	 ****/
	public void ProcessDialedResultCase(String callResponseStr)
	{
		callApiObject.sendReplyToAgent(
				CommonUtils.sendExtensionErrorInfo(
						callResponseStr,
						callApiObject.getExtNum()
				)
		);
	}


	public void onDispose() {
	}
}
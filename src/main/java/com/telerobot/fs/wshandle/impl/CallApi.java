package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.telerobot.fs.config.CallConfig;
import com.telerobot.fs.entity.bo.ChanneState;
import com.telerobot.fs.entity.bo.ChannelFlag;
import com.telerobot.fs.entity.dto.CallMonitorInfo;
import com.telerobot.fs.entity.dto.GatewayConfig;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.*;
import com.telerobot.fs.wshandle.*;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
//import com.telerobot.fs.predictiveCall.CallConfig;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  外呼控制;
 *  控制电话外呼、挂机;
 * @author easycallcenter365@gmail.com
 */
public class CallApi extends MsgHandlerBase {

	private CallListener listener = null;

    /**
     *  转接座席超时时间
     */
    private static int transferAgentTimeOut = Integer.parseInt(
            SystemConfig.getValue("inbound-transfer-agent-timeout", "30")
    );

    private static  boolean hidePhoneNumber = Boolean.parseBoolean(
            SystemConfig.getValue("hide-inbound-number", "true")
    );

    private static boolean callMonitorEnabled =  Boolean.parseBoolean(
            SystemConfig.getValue("call_monitor_enabled", "false")
    );

	private CallApi thisRef = this;

	@Override
	public void processTask(MsgStruct data) {
		MessageResponse msg = new MessageResponse();
		CallArgs callArgs = null;
		try {
			callArgs = JSON.parseObject(data.getBody(), CallArgs.class);
		} catch (Throwable e) {
			msg.setStatus(400);
			msg.setMsg("invalid json format.");
			sendReplyToAgent(msg);
			return;
		}
		if (callArgs == null) {
			return;
		}
		logger.info("{} recv msg: CallApi: {}", getTraceId(), data.getBody());
		String cmd = callArgs.getCmd();
		if (cmd == null || cmd.length() == 0) {
			Utils.processArgsError("cmd param error", thisRef);
			return;
		}
		switch (cmd) {
			case "startSession":
				startCall(callArgs);
				break;
            case "endSession":
                endCall();
                break;
            case "transferCall":
                transferCall(callArgs);
                break;
            case "reInviteVideo":
                reInviteVideo();
                break;
            case "playMp4File":
                playMp4File(callArgs);
                break;
			default:
				msg.setStatus(400);
				msg.setMsg(String.format("method not support :%s", cmd));
				sendReplyToAgent(msg);
				break;
		}
	}



	private void playMp4File(CallArgs callArgs){
	    String filePath = callArgs.getArgs().getString("mp4FilePath");
        if (this.listener != null && !StringUtils.isNullOrEmpty(filePath)) {
            this.listener.playMp4File(filePath);
        }
    }

    /**
     *  执行重邀请: reInvite 实现语音通话转视频通话
     */
	private void reInviteVideo(){
        if (this.getIsDisposed()) {
            return;
        }
        if (this.listener != null) {
            this.listener.reInviteVideo(this.getSessionInfo());
        }else{
            Utils.processArgsError("call session not exists.", thisRef);
        }
    }

    private void doTransferCall(String fromOpNum, SwitchChannel customerChannel) {
        SwitchChannel agentChannel = new SwitchChannel(
                "",
                customerChannel.getUuid(),
                customerChannel.getCallType(),
                customerChannel.getCallDirection()
        );
        agentChannel.setPhoneNumber(getExtNum());
        agentChannel.setPhoneNumber(customerChannel.getPhoneNumber());
        agentChannel.setBridgeCallAfterPark(true);
        agentChannel.setFlag(ChannelFlag.TRANSFER_CALL_RECV);
        agentChannel.setSendChannelStatusToWsClient(true);

        this.connectExtension(agentChannel, customerChannel);

        if(agentChannel.getAnsweredTime() > 0){
          // notify the sender, call is transferred successfully
            MessageHandlerEngine engine = MessageHandlerEngineList.getInstance().getMsgHandlerEngineByOpNum(fromOpNum);
            if (null != engine) {
                engine.sendReplyToAgent(new MessageResponse(
                        RespStatus.TRANSFER_CALL_SUCCESS,
                        "call transferred successfully."
                ));
            }
        }
    }

    /**
     * 尝试接通分机，直到超时
     * @param agentChannel
     * @param customerChannel
     */
    protected void connectExtension(SwitchChannel agentChannel, SwitchChannel customerChannel){
        if(customerChannel.getCallDirection().equalsIgnoreCase(CallDirection.OUTBOUND)){
            // disable customerChannel park event.
            customerChannel.setBridgeCallAfterPark(false);
        }

        listener = new CallListener(this, agentChannel, customerChannel);
        CallMonitorInfo callMonitorInfo = new CallMonitorInfo(
                agentChannel.getUuidBLeg(),
                agentChannel.getUuid(),
                getExtNum(),
                customerChannel.getPhoneNumber(),
                agentChannel.getCallType(),
                customerChannel.getCallDirection(),
                this.getSessionInfo().getGroupId(),
                System.currentTimeMillis()
        );
        if(callMonitorEnabled) {
            agentChannel.setCallMonitorEnabled(true);
            agentChannel.setCallMonitorInfo(callMonitorInfo);
        }

        EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
        boolean videoCall = agentChannel.getCallType().equals(PhoneCallType.VIDEO_CALL);
        String originationParam = String.format("originate_timeout=%s,hangup_after_bridge=false,origination_uuid={uuid},%s,ignore_early_media=true,not_save_record_flag=1",
                String.valueOf(transferAgentTimeOut),
                videoCall ?  "rtp_force_video_fmtp='profile-level-id=42e01e;packetization-mode=1',record_concat_video=true" : "absolute_codec_string=pcma"
        );
        String displayNumber = hidePhoneNumber ?
                CommonUtils.hiddenPhoneNumber(customerChannel.getPhoneNumber()) : customerChannel.getPhoneNumber();
        String callerInfo = String.format(
                "extnum=%s,origination_caller_id_number=%s,origination_caller_id_name=%s,effective_caller_id_number=%s,effective_caller_id_name=%s",
                this.getExtNum(),
                displayNumber,
                displayNumber,
                displayNumber,
                displayNumber
        );

        // 确保接通分机
        long startTime = System.currentTimeMillis();
        AtomicInteger bLegIndex = new AtomicInteger(0);
        while (agentChannel.getAnsweredTime() == 0L && customerChannel.getHangupTime() == 0L && !getIsDisposed()) {
            long passedTime = System.currentTimeMillis() - startTime;
            if(passedTime > transferAgentTimeOut * 1000){
                logger.info("{}  transfer call {} to extension {} timeout.",
                        getTraceId(), customerChannel.getUuid(), getExtNum());
                break;
            }
            logger.info("{} try to connect extension {}, tryTimePassed={}",
                    getTraceId(), this.getExtNum(), passedTime);

            int index = bLegIndex.incrementAndGet();
            agentChannel.setHangupTime(0L);
            agentChannel.setUuid(String.format("%s-%s-bleg-%d",
                    customerChannel.getUuid(), getExtNum(), index));
            customerChannel.setUuidBLeg(agentChannel.getUuid());
            connectionPool.getDefaultEslConn().addListener(agentChannel.getUuid() + "-ex", listener);
            connectionPool.getDefaultEslConn().addListener(customerChannel.getUuid() + "-ex", listener);
            String bgApiUuid = EslConnectionUtil.sendAsyncApiCommand(
                    "originate",
                    String.format("{%s,%s}user/%s &park()",
                            originationParam.replace("{uuid}", agentChannel.getUuid()),
                            callerInfo,
                            this.getExtNum()
                    ),
                    connectionPool
            );
            if(!StringUtils.isNullOrEmpty(bgApiUuid)) {
                logger.info("{} doTransferCall connect extension {}, async jobId={}",
                        getTraceId(), getExtNum(),bgApiUuid);
                connectionPool.getDefaultEslConn().addListener(bgApiUuid.trim(), listener);
                this.listener.setBackgroundJobUuid(bgApiUuid.trim());
            }

            listener.waitForSignal();
            if (agentChannel.getAnsweredTime() == 0L
                    || agentChannel.getHangupTime() > 0L ) {
                logger.error("{} extension no answer, try again.", getTraceId());
                ThreadUtil.sleep(1000);
            }
        }
    }

    private void transferCall(CallArgs callArgs) {
	    if(this.listener == null){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "no call session found."
            ));
	        return;
        }
        String from = this.getSessionInfo().getOpNum();
        String to = callArgs.getArgs().getString("to");
        if(from.equalsIgnoreCase(to)){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "can not transfer call to yourself."
            ));
            return;
        }
        if(StringUtils.isNullOrEmpty(to)){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "'to' argument is null in transferCall."
            ));
            return;
        }

        SwitchChannel customerChannel = listener.getCustomerChannel();
        if(customerChannel.getHangupTime() > 0L || customerChannel.getAnsweredTime() == 0L ){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "call is hangup or not ready."
            ));
            return;
        }

        MessageHandlerEngine engine = MessageHandlerEngineList.getInstance().getMsgHandlerEngineByOpNum(to);
        if (null != engine) {
            if(engine.getSessionInfo() == null || !engine.getSessionInfo().tryLock()){
                sendReplyToAgent(new MessageResponse(
                        RespStatus.LOCK_AGENT_FAIL,
                        "lock agent failed."
                ));
                return;
            }

            CallApi callApi = ((CallApi) engine.getMessageHandleByName("call"));
            if (null != callApi) {

                // HOLD Customer CALL
                EslConnectionUtil.sendExecuteCommand(
                        "set",
                        "park_after_bridge=true",
                        customerChannel.getUuid()
                );
                customerChannel.setFlag(ChannelFlag.HOLD_CALL);
                logger.info("{} set customerChannel {} HOLD_CALL and park_after_bridge=true",
                        getTraceId(), customerChannel.getUuid());
                ThreadUtil.sleep(10);

                //挂断转出电话的分机
                this.listener.endCall("call_transferred.");
                ThreadUtil.sleep(10);

                //发送弹屏消息
                engine.sendReplyToAgent(new MessageResponse(
                        RespStatus.TRANSFER_CALL_RECV, "转接的来电请求", customerChannel)
                );
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", AgentStatus.busy.getIndex());
                // 座席置忙
                engine.sendReplyToAgent(new MessageResponse(
                        RespStatus.STATUS_CHANGED, "当前用户状态: 忙碌", jsonObject)
                );

                logger.info("{} 设定座席的忙碌锁定状态. userId={}, extNum={}",
                        callApi.getTraceId(),
                        to,
                        callApi.getExtNum()
                );
                AppContextProvider.getBean(SysService.class).setAgentStatusWithBusyLock(
                        to, AgentStatus.busy.getIndex()
                );

                callApi.doTransferCall(from, customerChannel);
            } else {
                engine.sendReplyToAgent(new MessageResponse(
                        RespStatus.SERVER_ERROR, "server internal error!", customerChannel)
                );
            }
        }else{
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "Destination agent user is offline."
            ));
        }
    }

	/**
	 *  结束通话：
	 */
	private void endCall() {
		if (this.getIsDisposed()) {
			return;
		}

		if (this.listener != null) {
            this.listener.clearCustomerHoldCallFlag();
			this.listener.endCall();
		}else{
			Utils.processArgsError("通话不存在.", thisRef);
		}
	}

    private String fullRecordPath = "";

	private static final  String CALL_TEMPLATE_STRING =
               "hangup_after_bridge=false,park_after_bridge=true,"
			+  "cc_call_leg_uuid=%s,"
			+ "origination_uuid=%s,"
			+ "%s"
			+ "RECORD_STEREO=%s,"
			+ "origination_caller_id_number=%s,"
			+ "effective_caller_id_number=%s,"
			+ "caseno=%s,"
			+ "callee=%s,"
			+ "caller=%s,"
			+ "extNum=%s,"
			+ "opNum=%s,"
			+ "callId=%s,"
			+ "role=%s,"
			+ "projectId=%s,"
			+ "fullRecordPath=%s,"
			+ "record_waste_resources=true,"
			+ "record_sample_rate=8000%s";


    /**
     * 外呼之前，先接通座席分机
     */
    public boolean connectCallExtNum(CallListener callListener, SwitchChannel agentChannel,
                                  SwitchChannel customerChannel, String projectId, String caseNo){
        if(null == callListener) {
            listener = new CallListener(this, agentChannel, customerChannel);
        }else{
            listener = callListener;
        }

        EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
        connectionPool.getDefaultEslConn().addListener(agentChannel.getUuid(), listener);
        String callExtensionStr = genCallExtensionStr(projectId, caseNo,
                agentChannel, customerChannel.getPhoneNumber());
        logger.info("{} callExtensionStr = {}", getTraceId(), callExtensionStr);

        String jobId = EslConnectionUtil.sendAsyncApiCommand(
                "originate",
                callExtensionStr
        );

        logger.info("{} callExtension response: {}", getTraceId(), jobId);
        if (!StringUtils.isNullOrEmpty(jobId)) {
            connectionPool.getDefaultEslConn().addListener(jobId.trim(), listener);
            this.listener.setBackgroundJobUuid(jobId.trim());
        } else {
            logger.error("{} callExtension cant not get fs backGroundJobUuid", getTraceId());
        }

        listener.waitForSignal();

        // 超时未接听，自动结束通话
        if (agentChannel.getAnsweredTime() == 0L || agentChannel.getHangupTime() > 0L) {
            sendReplyToAgent(new MessageResponse(
                    RespStatus.CALLER_RESPOND_TIMEOUT, "extension no reply, please check extension is login.")
            );
            return false;
        }
        return  true;
    }

	private String genCallExtensionStr(String projectId, String caseNo, SwitchChannel channel, String phone){
        String extNum = this.getExtNum();
        String uuidInner = channel.getUuid();
        String callType = channel.getCallType();
        String videoLevel = channel.getVideoLevel();
	    String caller = extNum;
		String callee = phone;
		String phoneHiddenStr = CommonUtils.hiddenPhoneNumber(phone);
		String enableCcRecordStereo = SystemConfig.getValue("enable_cc_record_stereo", "false");
		String callPrefixInnerLine = String.format(CALL_TEMPLATE_STRING,
				uuidInner,
				uuidInner,
                PhoneCallType.checkAudioCall(callType) ?  ("absolute_codec_string=pcma,") : "",
				enableCcRecordStereo,
				phoneHiddenStr,
				phoneHiddenStr,
				caseNo,
				callee,
				caller,
				extNum,
                getSessionInfo().getOpNum(),
				uuidInner,
				"2",
				projectId,
				fullRecordPath,
				PhoneCallType.checkVideoCall(callType) ?
						",rtp_force_video_fmtp='profile-level-id="+ videoLevel.trim() +";packetization-mode=1',record_concat_video=true" : ""
		);
		return String.format("{%s,sip_auto_answer=true,not_save_record_flag=1}user/%s  &park",
				callPrefixInnerLine,
				extNum
		);
	}

    private String genCallPhoneString(GatewayConfig gatewayConfig, String projectId, String caseNo,
                                 String uuidInner, String uuidOuter, String phone,
                                 String callType, String videoLevel)
    {
        String extNum = this.getExtNum();
        String caller = extNum;
        String callee = phone;
        String enableCcRecordStereo = SystemConfig.getValue("enable_cc_record_stereo", "false");

        // 外呼网关地址
        String gatewayAddress = gatewayConfig.getGatewayAddr();
        // 主叫号码
        String callerNumber = gatewayConfig.getCallerNumber();
        // 被叫前缀
        String calleePrefix = gatewayConfig.getCalleePrefix();
        String sipProfile = gatewayConfig.getCallProfile();
        String codec = gatewayConfig.getAudioCodec();

        String callPrefixOuterLine = String.format(CALL_TEMPLATE_STRING,
                uuidInner,
                uuidOuter,
                PhoneCallType.checkAudioCall(callType) ?   ("absolute_codec_string=" + codec + ",")  : "",
                enableCcRecordStereo,
                callerNumber,
                callerNumber,
                caseNo,
                callee,
                caller,
                extNum,
                getSessionInfo().getOpNum(),
                uuidOuter,
                "1",
                projectId,
                fullRecordPath,
                PhoneCallType.checkVideoCall(callType) ?
                        ",rtp_force_video_fmtp='profile-level-id="+ videoLevel.trim() +";packetization-mode=1',record_concat_video=true" : ""
        );

        // 对接模式和注册模式，bridge字符串拼接内容不同;
        String bridgeString = String.format("{execute_on_answer='record_session %s',%s}sofia/%s/%s%s@%s  &park",
                CallConfig.RECORDINGS_PATH + fullRecordPath,
                callPrefixOuterLine,
                sipProfile,
                calleePrefix,
                phone,
                gatewayAddress
        );

        if(gatewayConfig.getRegister()){
            bridgeString = String.format("{execute_on_answer='record_session %s',%s}sofia/gateway/%s/%s%s  &park",
                    CallConfig.RECORDINGS_PATH + fullRecordPath,
                    callPrefixOuterLine,
                    gatewayAddress,
                    calleePrefix,
                    phone
            );
        }

        return bridgeString;
    }



	/**
	 *  开始通话：
	 */
	private void startCall(CallArgs callArgs) {
        String gatewayListArgs = null;
        boolean gatewayEncrypted = callArgs.getArgs().getBoolean("gatewayEncrypted");
        // audio or video
        String callType = callArgs.getArgs().getString("callType");
        String videoLevel = callArgs.getArgs().getString("videoLevel");
        boolean useSameAudioCodeForOutbound = callArgs.getArgs().getBoolean("useSameAudioCodeForOutbound");
        this.fullRecordPath = "";

        if (!PhoneCallType.checkCallTypeValid(callType)) {
            callType = PhoneCallType.AUDIO_CALL;
            logger.info("{} 由于参数传递错误，已经自动把 callType 参数设置为 {}", getTraceId(), callType);
        }

        if (PhoneCallType.checkVideoCall(callType)) {
            if (StringUtils.isNullOrEmpty(videoLevel)) {
                logger.info("已经自动把 videoLevel 参数设置为 {}", VideoConfigs.DEFAULT_VIDEO_LEVEL);
            }
            if (!VideoConfigs.checkVideoLevels(videoLevel)) {
                Utils.processArgsError("UnSupported videoLevel " + videoLevel, thisRef);
                return;
            }
        }

        if (gatewayEncrypted) {
            try {
                gatewayListArgs = DESUtil.decrypt(callArgs.getArgs().getString("gatewayList"));
            } catch (Throwable e) {
                logger.error("解密网关列表数据失败: {} {}", e.toString(), JSON.toJSONString(e.getStackTrace()));
                Utils.processArgsError("gatewayList parameter error!", thisRef);
                return;
            }
        } else {
            gatewayListArgs = callArgs.getArgs().getString("gatewayList");
        }

        List<GatewayConfig> gatewayList = JSON.parseObject(gatewayListArgs, new TypeReference<List<GatewayConfig>>() {
        });
        logger.info("gatewayListArgs={}", gatewayListArgs);
        if (null == gatewayList || gatewayList.size() == 0) {
            Utils.processArgsError("gatewayList parameter error!", thisRef);
            return;
        }

        String phoneAndCaseInfo = callArgs.getArgs().getString("destPhone");
        String caseNo = "";
        String phone = "";
        if (phoneAndCaseInfo.contains(";")) {
            caseNo = phoneAndCaseInfo.split(";")[1];
            phone = phoneAndCaseInfo.split(";")[0];
        } else {
            caseNo = "notSet";
            phone = phoneAndCaseInfo;
        }

        boolean isNumeric = StringUtils.isNumeric(phone.replace(" ", "").replace("-", "").trim());
        // 电话号码不是数字的情况下，需要解密号码字符串
        if (!isNumeric) {
            String key = SystemConfig.getValue("phone_encrypted_key");
            phone = EncryptUtil.getInstance().DESdecode(phone, key);
            if (null == phone) {
                Utils.processArgsError("手机号码解密失败，请检查解密的密码字符串", thisRef);
                return;
            }
        }

        String uuidInner = UuidGenerator.GetOneUuid();
        String uuidOuter = UuidGenerator.GetOneUuid();
        SwitchChannel agentChannel = new SwitchChannel(uuidInner, uuidOuter, callType, CallDirection.OUTBOUND);
        SwitchChannel customerChannel = new SwitchChannel(uuidOuter, uuidInner, callType, CallDirection.OUTBOUND);

        agentChannel.setVideoLevel(videoLevel);
        agentChannel.setPhoneNumber(this.getExtNum());
        agentChannel.setSendChannelStatusToWsClient(true);
        customerChannel.setVideoLevel(videoLevel);
        customerChannel.setPhoneNumber(phone);
        customerChannel.setBridgeCallAfterPark(true);
        customerChannel.setSendChannelStatusToWsClient(true);
        CallMonitorInfo callMonitorInfo = new CallMonitorInfo(
                uuidOuter,
                uuidInner,
                getExtNum(),
                phone,
                callType,
                CallDirection.OUTBOUND,
                this.getSessionInfo().getGroupId(),
                System.currentTimeMillis()
        );
        if (callMonitorEnabled) {
            customerChannel.setCallMonitorEnabled(true);
            customerChannel.setCallMonitorInfo(callMonitorInfo);
        }

        // 项目编号
        String projectId = this.getSessionInfo().getGroupId();
        String recordingsType = SystemConfig.getValue("recordings_extension", "wav");
        if (PhoneCallType.checkVideoCall(callType)) {
            recordingsType = "mp4";
        }

        String recordFieName = getExtNum() + "_" + phone + "_" + uuidInner + "." + recordingsType;
        fullRecordPath = projectId
                + DateUtils.format(new Date(), "/yyyy/MM/dd/HH/")
                + recordFieName;

        logger.info("{} set fullRecordPath={}{}", getTraceId(), CallConfig.RECORDINGS_PATH, fullRecordPath);
        customerChannel.setRecordingFilePath(fullRecordPath);

        //首先接通分机
        boolean callExtSuccess = this.connectCallExtNum(null, agentChannel, customerChannel, projectId, caseNo);
        if (!callExtSuccess) {
            return;
        }

        //记录已经尝试过的网关;
        List<GatewayConfig> triedList = new ArrayList<>(10);
        EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
        if (connectionPool != null) {

            boolean firstCall = true;
            do {
                if (!firstCall) {
                    logger.info("{} call failed sipCode={}，retry again",
                            getTraceId(), customerChannel.getHangupSipCode());
                    ThreadUtil.sleep(100);
                }
                firstCall = false;

                listener.resetCustomerChannelLastCallStatus();

                GatewayConfig gatewayConfig = SipGatewayLoadBalance.getGateway(gatewayList, triedList);
                if (null == gatewayConfig) {
                    logger.info("{} no available gateway, exit the outgoing call attempt! Number of tried gateways={}", getTraceId(), triedList.size());
                    this.sendReplyToAgent(
                            new MessageResponse(
                                    RespStatus.OUTBOUND_FINISHED,
                                    "No available gateway, Number of tried gateways: " + triedList.size()
                            )
                    );
                    break;
                }
                logger.info("{} successfully get a gateway: {} ", getTraceId(), JSON.toJSONString(gatewayConfig));

                String originationStr = genCallPhoneString(gatewayConfig, projectId, caseNo, uuidInner,
                        uuidOuter, phone, callType, videoLevel);


                IOnHangupHook hangupHook = new IOnHangupHook() {
                    @Override
                    public void onHangup(Map<String, String> headers, String traceId) {
                        SipGatewayLoadBalance.releaseGateway(gatewayConfig);
                        logger.info("{} releaseGateway gatewayUuid={}, gatewayAddr={}",
                                traceId, gatewayConfig.getUuid(), gatewayConfig.getGatewayAddr()
                        );
                    }
                };
                customerChannel.setHangupHook(hangupHook);

                connectionPool.getDefaultEslConn().addListener(uuidOuter, listener);
                logger.info("{} originationStr: originate {}", getTraceId(), originationStr);
                String jobId = EslConnectionUtil.sendAsyncApiCommand("originate", originationStr, connectionPool);
                logger.info("{} fs bgapi originate response: {}", getTraceId(), jobId);
                if (!StringUtils.isNullOrEmpty(jobId)) {
                    connectionPool.getDefaultEslConn().addListener(jobId.trim(), listener);
                    this.listener.setBackgroundJobUuid(jobId.trim());
                } else {
                    logger.error("{}  cant not get FreeSWITCH backGroundJobUuid", getTraceId());
                }

                listener.waitForSignal();
                if (super.getIsDisposed()) {
                    break;
                }

                if (!listener.checkCustomerChannelCallStatus()) {
                    logger.info("{} call originate failed，add current gateway to  triedList： gateway = {}  ", getTraceId(), gatewayConfig);
                    triedList.add(gatewayConfig);
                } else {
                    logger.info("{} call originate finished successfully，tried gateway list count：{} , details: {}", getTraceId(), triedList.size(), triedList);
                }
            } while (!listener.checkCustomerChannelCallStatus() &&
                    agentChannel.getHangupTime() == 0L && !getIsDisposed());

            logger.info("{} Call finished.", getTraceId());
            if (!listener.checkCustomerChannelCallStatus()) {
                logger.info("{} hangup extension {} due to no outbound call connected.", getTraceId(), getSessionInfo().getExtNum());
                endCall();
            }

        } else {
            Utils.processServerInternalError("Can not connect to freeSwitch.", this, true);
        }
    }

	public void bridgeCall(SwitchChannel agentChannel, SwitchChannel customerChannel){
        String uuidInner = agentChannel.getUuid();
        String uuidOuter = customerChannel.getUuid();
        logger.info("{} try to bridge call,  uuidInner={}, uuidOuter={}",
                getTraceId(), uuidInner, uuidOuter
        );

        EslMessage eslMessage = EslConnectionUtil.sendSyncApiCommand(
                "uuid_bridge",
                String.format("%s %s",uuidInner, uuidOuter),
                EslConnectionUtil.getDefaultEslConnectionPool()
        );

        boolean bridgeSucceed = false;
        if(eslMessage.getBodyLines().size() > 0){
            if(eslMessage.getBodyLines().get(0).contains("+OK")){
                bridgeSucceed = true;
                agentChannel.setChannelState(ChanneState.BRIDGED);
                customerChannel.setChannelState(ChanneState.BRIDGED);
            }
        }

        if(!bridgeSucceed){
            logger.error("{} call bridged failed： {}", getTraceId(), JSON.toJSONString(eslMessage));
        }else{
            logger.info("{} call bridged successfully： {}", getTraceId(), JSON.toJSONString(eslMessage));

        }
    }


	/**
	 * Acd转坐席之前，先接通座席分机
	 */
	public String connectAgentExtNum(SwitchChannel agentChannel, SwitchChannel customerChannel,
	        String displayNumber,int transferAgentTimeOut, Long inboundTime){

		listener = new CallListener(this, agentChannel, customerChannel);

        CallMonitorInfo callMonitorInfo = new CallMonitorInfo(
                customerChannel.getUuid(),
                customerChannel.getUuidBLeg(),
                getExtNum(),
                customerChannel.getPhoneNumber(),
                agentChannel.getCallType(),
                CallDirection.INBOUND,
                this.getSessionInfo().getGroupId(),
                inboundTime

        );
        if(callMonitorEnabled) {
            agentChannel.setCallMonitorEnabled(true);
            agentChannel.setCallMonitorInfo(callMonitorInfo);
        }
        agentChannel.setBridgeCallAfterPark(true);
        agentChannel.setSendChannelStatusToWsClient(true);

		EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
		// 当前监听器是扩展分发的esl消息; 采用 -ex 后缀，避免覆盖主监听器：
		connectionPool.getDefaultEslConn().addListener(agentChannel.getUuid() + "-ex", listener);
		connectionPool.getDefaultEslConn().addListener(customerChannel.getUuid() + "-ex", listener);

		boolean videoCall = agentChannel.getCallType().equals(PhoneCallType.VIDEO_CALL);
		String originationParam = String.format("originate_timeout=%s,hangup_after_bridge=false,origination_uuid=%s,%s,ignore_early_media=true,not_save_record_flag=1",
				String.valueOf(transferAgentTimeOut),
                agentChannel.getUuid(),
                videoCall ?  "record_concat_video=true" : "absolute_codec_string=pcma"
                // rtp_force_video_fmtp='profile-level-id=42e01e;packetization-mode=1',
		);
 		String callerInfo = String.format(
				"extnum=%s,origination_caller_id_number=%s,origination_caller_id_name=%s,effective_caller_id_number=%s,effective_caller_id_name=%s",
				this.getExtNum(),
				displayNumber,
				displayNumber,
				displayNumber,
				displayNumber
		);
		return EslConnectionUtil.sendAsyncApiCommand(
				"originate",
				String.format("{%s,%s}user/%s &park()",
						originationParam,
						callerInfo,
						this.getExtNum()
				),
				connectionPool
		);
	}

	@Override
	public void dispose() {
        if(null != this.listener){
        	// 业务对象销毁的时候，默认会挂断当前通话;
			if(Boolean.parseBoolean(AppContextProvider.getEnvConfig("app-config.phone-bar.end-call-on-websocket-disconnect", "false"))) {
				this.listener.endCall("on_dispose");
			}
		}
		super.dispose();
	}

	private class HandlerInitializer implements IMsgHandlerInitializer {
		@Override
		public void activeCurrentHandlerInstance() {
			logger.info("callApi actived ...");
		}
		@Override
		public void destroyHandlerInstance() {
			if (listener != null) {
				listener.onDispose();
			}
			logger.info(" {} callApi object for user {} is destroyed.",
					getTraceId(), getExtNum());
		}
	}
	@Override
	public void activeCurrentObject(MessageHandlerEngine msgHandlerEngine, IMsgHandlerInitializer... initializer) {
	       super.activeCurrentObject(msgHandlerEngine, new HandlerInitializer());
	}

	public String getExtNum() {
		return this.getSessionInfo().getExtNum();
	}
}

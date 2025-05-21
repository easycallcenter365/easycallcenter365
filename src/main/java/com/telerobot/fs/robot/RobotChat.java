package com.telerobot.fs.robot;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.acd.AcdSqlQueue;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.ThreadLocalTraceId;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.entity.pojo.LlmToolRequest;
import com.telerobot.fs.entity.pojo.SpeechResultEntity;
import com.telerobot.fs.service.InboundDetailService;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import io.netty.util.internal.StringUtil;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.transport.message.EslMessage;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author easycallcenter365@126.com 
 */
public class RobotChat extends RobotBase {
    private static int initApp = init();
    private static int init() {
        int maxRobotNumber = Integer.parseInt(SystemConfig.getValue("max-call-concurrency")) ;
        startRobotMainThreadPool(maxRobotNumber);
        startFsEsNotifyThreadPool(maxRobotNumber);
        startRobotStatThread();
        return 1;
    }
    public static int getInit() {
        return initApp;
    }

    public RobotChat(InboundDetail callDetail) {
        this.setAsrModelType(SystemConfig.getValue("robot-asr-type", ASR_TYPE_MRCP));
        logger.info("{} current robot_asr_type={}.",
                getTraceId(),  this.getAsrModelType()
        );
        if(getAllowInterrupt() && ASR_TYPE_MRCP.equalsIgnoreCase(this.getAsrModelType())){
           logger.error("{} `robot-speech-interrupt-allowed=true` parameter is not effective in the mrcp speech recognition mode.", uuid);
        }

        this.callDetail = callDetail;
        this.uuid = callDetail.getUuid();
        getEslConnectionPool(
                uuid,
                SystemConfig.getValue("event-socket-ip"),
                Integer.parseInt(SystemConfig.getValue("event-socket-port"))
        );
        callDetail.setAnsweredTime(System.currentTimeMillis());
        AcdSqlQueue.addToSqlQueue(callDetail);
        callTaskList.put(uuid, this);
        createChatBot();
        chatRobot.setCallDetail(callDetail);
    }

    public void startProcess(String uniqueID) {
        EslMessage apiResponseMsg = EslConnectionUtil.sendSyncApiCommand(
                "uuid_exists",
                uniqueID,
                this.eslConnectionPool
        );
        if (apiResponseMsg != null && apiResponseMsg.getBodyLines().size() != 0) {
            String apiResponseText = apiResponseMsg.getBodyLines().get(0);
            if ("false".equalsIgnoreCase(apiResponseText)) {
                logger.info("{} session is hangup，stop robot process.", getTraceId());
                this.processFsMsg(this.generateHangupEvent("hangup-before-robot-process"));
                return;
            }
        } else {
            logger.info("{} uuid_exists check error, can not get apiResponseMsg...", getTraceId());
        }

        logger.info("{} startProcess...", getTraceId());

        startAsrProcess(getAsrModelType(), false);

        interactWithRobot("你好。");
    }

    protected void processFsMsgEx(Map<String, String> headers) {
        String eventName = headers.get("Event-Name");

        String eventDateTimestamp = headers.get("Event-Date-Timestamp");
        if(null != eventDateTimestamp) {
            // esl消息从产生到被处理的延迟时间; 毫秒数
            long eventTime = Long.parseLong(eventDateTimestamp) / 1000L;
            long timeDelay = System.currentTimeMillis() - eventTime;
            logger.info("{} The [{}] event takes {} ms from generation to processing.", getTraceId(), eventName, timeDelay);
        }

        String eventSubClass = headers.get("Event-Subclass");
        logger.info("{}  Event-Name：{} ", getTraceId(), eventName);
        if(EventNames.PLAYBACK_START.equalsIgnoreCase(eventName)){

            long timeSpent =  System.currentTimeMillis() - playbackStartTime;
            logger.info("{} PLAYBACK_START event,  time cost = {} ms. ", getTraceId(), timeSpent);

        }else if (EventNames.PLAYBACK_STOP.equalsIgnoreCase(eventName)) {
            recvPlayBackEndEvent = true;
            playbackEndTime = System.currentTimeMillis();
            releaseSignal();
            logger.info("{} streaming tts playback finished.", getTraceId());

             if(recvHangupSignal){
                 EslConnectionUtil.sendExecuteCommand(
                         "hangup",
                         "recvHangupSignal",
                         uuid,
                         this.eslConnectionPool
                 );
             }
        }else if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
            if(isHangup){
                return;
            }
            releaseSignal();
            isHangup = true;
            // 挂机后立即释放通道
            releaseThreadNum();
            releaseDtmf();
            displayNoVoiceNum();

            // 保存催记内容
            try {
                saveCdr(headers);
            } catch (Throwable e) {
                logger.info("{}  save cdr error: {}, {} ", getTraceId(), e.toString(),
                        CommonUtils.getStackTraceString(e.getStackTrace()));
            }
        } else if ("CUSTOM".equalsIgnoreCase(eventName) && (
                "TtsEvent".equalsIgnoreCase(eventSubClass)
        )) {
           String event = headers.get("Tts-Event-Detail");
           if("Speech-Closed".equalsIgnoreCase(event)){
               chatRobot.setTtsChannelState(true);
               logger.info("{}  TtsChannelClosed = true.", getTraceId());
           }
        }
        else if ("CUSTOM".equalsIgnoreCase(eventName) && (
                "AsrEvent".equalsIgnoreCase(eventSubClass)
         )) {
            String speechEvent = headers.get("ASR-Event-Detail");
            String asrResponse = headers.get("Detect-Speech-Result");
            if (null != asrResponse) {
                asrResponse = headers.get("Detect-Speech-Result").trim();
            }
            lastTalkTime = System.currentTimeMillis();

            if (isHangup || interactiveParam.checkInHangupState()) {
                logger.info("{} Session is going to be hangup, drop asr result: {}", getTraceId(), asrResponse);
                return;
            }

            if (!getAllowInterrupt() && !recvPlayBackEndEvent) {
                interruptRobot = true;
                if ("vad".equalsIgnoreCase(speechEvent)) {
                    dropAsrCounter.incrementAndGet();
                    logger.info("{} (vad event) drop asr result: {}", getTraceId(), asrResponse);
                } else {
                    logger.info("{} (vad event) drop asr result: {}", getTraceId(), asrResponse);
                }
                return;
            }
            if ("middle".equalsIgnoreCase(speechEvent)) {
                logger.info("{}  ** asr-websocket, begin-speaking **  {}", getTraceId(), asrResponse);
                if (recvPlayBackEndEvent || getAllowInterrupt()) {
                    if (!interactiveParam.checkInSpeaking()) {
                        synchronized (getTraceId().intern()) {
                            if (!interactiveParam.checkInSpeaking()) {
                                interruptRobotSpeech();
                                // 用户开始讲话标识
                                interactiveParam.setInSpeaking(true);
                                // 唤醒主线程，让主线程可以超出6秒限制;
                                releaseSignal();
                            }
                        }
                    }
                }
            } else if ("vad".equalsIgnoreCase(speechEvent)) {
                logger.info("{}  ** vad end-speaking:  {}", getTraceId(), asrResponse);

                if (!StringUtil.isNullOrEmpty(asrResponse)) {
                    asrResultEx.add(asrResponse);
                }

                if (recvPlayBackEndEvent || getAllowInterrupt()) {
                    logger.info("{} releaseSignal for vad event.", getTraceId());
                    interactiveParam.setInSpeaking(false);
                    releaseSignal();
                } else {
                    logger.info("{} Neither [playback finished] or  [AllowInterrupt] is true, No signal will be sent to main thread, recvPlayBackEndEvent=false, AllowInterrupt={} ",
                            getTraceId(), getAllowInterrupt());
                }

            }
        }
    }

    protected void interruptRobotSpeech(){
        logger.info("{} send uuid_break command to FreeSWITCH.", uuid);
        EslConnectionUtil.sendSyncApiCommand("uuid_break", uuid + " all");
    }

    @Override
    protected void processFsMsg(Map<String, String> headers) {
        try {
            processFsMsgEx(headers);
        } catch (Throwable e) {
            logger.error("{} processFsMsg error: {}, {}", getTraceId(), e.toString(),
                    CommonUtils.getStackTraceString(e.getStackTrace()));
        }
    }

    protected void processFsMsg2(EslEvent event) {
        Map<String, String> headers = event.getEventHeaders();
        String eventName = headers.get("Event-Name");
        if (eventName.equalsIgnoreCase("DETECTED_SPEECH")) {
            String speechEvent = headers.get("Speech-Type");
            lastTalkTime = System.currentTimeMillis();

            if (isHangup || interactiveParam.checkInHangupState()) {
                logger.info("{} Session is going to be hangup, drop mrcp asr result: {}", getTraceId(),
                        headers.get("detect_speech_result"));
                return;
            }

            if ("begin-speaking".equalsIgnoreCase(speechEvent)) {
                if (!interactiveParam.checkInSpeaking()) {
                    logger.info(getTraceId() + " ** customer  begin-speaking detected. **");
                    // 用户开始讲话标识
                    interactiveParam.setInSpeaking(true);
                    releaseSignal(); // 唤醒主线程，让主线程可以超出6秒限制;
                }
            } else  if ("detected-partial-speech".equalsIgnoreCase(speechEvent)) {
                synchronized (getTraceId().intern()) {
                    if (!interactiveParam.checkInSpeaking()) {
                        // 用户开始讲话标识
                        interactiveParam.setInSpeaking(true);
                        // 唤醒主线程，让主线程可以超出6秒限制;
                        releaseSignal();
                    }
                }
                // 语音识别的中间结果;
                logger.info("{} detected-partial-speech = {}", getTraceId(), headers.get("detect_speech_result"));

            } else if ("detected-speech".equalsIgnoreCase(speechEvent)) {
                if (!interactiveParam.checkInSpeaking()) {
                    logger.info(getTraceId() + " mrcp return, no asr result got. isInSpeaking=false.");
                } else {
                    logger.info(getTraceId() + " ****** customer stop-speaking detected. ******");
                }
                interactiveParam.setInSpeaking(false);
                String speechResult = headers.get("detect_speech_result");
                if(StringUtils.isEmpty(speechResult)){
                    speechResult = CommonUtils.ListToString(event.getEventBodyLines(), false);
                }
                if (StringUtils.isEmpty(speechResult) || "Completion-Cause: 002".equalsIgnoreCase(speechResult.trim())) {
                    logger.info(getTraceId() + "  mrcp return, no asr result got...");
                    releaseSignal();
                    return;
                }

                if(speechResult.startsWith("<?xml")){
                    parseMrcpResultXmlStr(speechResult);
                    logger.info("{} detect_speech_result: {}", getTraceId(), speechResult);
                }else{
                    try {
                        String tmpResult = URLDecoder.decode(speechResult,"utf-8").replace(" ","");
                        asrResultEx.add(tmpResult);
                        logger.info("{} kaldi asr response: {}",getTraceId(), tmpResult);
                    } catch (Throwable e) {
                        logger.error("{} URLDecoder.decode Error: {}", getTraceId(), speechResult);
                    }
                }
                releaseSignal();
            }
        }
    }

    /**
     *  解析unimrcp-client接收到的xml字符串;
     * @param xmlStr
     */
    private void parseMrcpResultXmlStr(String xmlStr){
        SpeechResultEntity resultEntity = null;
        try {
            String input = URLDecoder.decode(xmlStr, "utf-8");
            resultEntity = getSpeechResult(input);
        } catch (UnsupportedEncodingException e) {
        }
        if (null != resultEntity) {
            if (!StringUtils.isEmpty(resultEntity.getResult())) {
                String asrResult = resultEntity.getResult();
                if(!StringUtil.isNullOrEmpty(asrResult)) {
                    asrResultEx.add(asrResult);
                }
                logger.info(getTraceId() + " mrcp asr result: {}, requestid: {}", resultEntity.getResult(), resultEntity.getRequestId());
            }
        } else {
            logger.info(getTraceId() + " cant not parse variable  detect_speech_result.");
        }
    }

    @Override
    public void eventReceived(String addr, EslEvent event) {
        fsEsNotifyThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> headers = event.getEventHeaders();
                boolean mrcpResult = EventNames.DETECTED_SPEECH.equalsIgnoreCase(headers.get("Event-Name"));
                if (getAsrModelType().equalsIgnoreCase(ASR_TYPE_MRCP) && mrcpResult) {
                    processFsMsg2(event);
                } else {
                    // output of FreeSWITCH websocket asr module
                    processFsMsg(headers);
                }
            }
        });
    }

    @Override
    public void backgroundJobResultReceived(String addr, EslEvent event) {
    }

    /**
     * interactWithRobot
     **/
    private void interactWithRobot(String greeting) {
        ThreadLocalTraceId.getInstance().setTraceId(getTraceId());
        interactiveParam.setAllowInterrupt(0);
        recvPlayBackEndEvent = false;
        // 设置合成录音文件的路径，返回给Robot
        if (asrResultEx.size() == 0 && StringUtils.isEmpty(greeting)) {
            asrResultEx.add("客户没有说话。");
        }
        if(!StringUtils.isEmpty(greeting)){
            asrResultEx.clear();
            asrResultEx.add(greeting);
        }

        if(getAsrModelType().equalsIgnoreCase(ASR_TYPE_WEBSOCKET)) {
            pauseAsr();
        }

        // 送robot理解客户意图，返回合成后的语音文件路径
        StringBuilder asrStr = new StringBuilder();
        for (String result : asrResultEx) {
            asrStr.append(result);
        }

        // 清空 asrResultEx; 重新初始化字段;
        asrResultEx.clear();

        // 识别开始时间
        Long startTime = System.currentTimeMillis();
        LlmAiphoneRes aiphoneRes;

            try {
                aiphoneRes = chatRobot.talkWithAiAgent(asrStr.toString());
                talkRound.increment();
                Long spentCost = System.currentTimeMillis() - startTime;
                logger.info("{}  talkWithLargeModel spent time:  {}  ms, aiphoneRes = {}",
                        getTraceId(), spentCost, JSON.toJSONString(aiphoneRes)
                );

                /*
                aiphoneRes = new  LlmAiphoneRes();
                aiphoneRes.setStatus_code(1);
                aiphoneRes.setClose_phone(0);
                aiphoneRes.setIfcan_interrupt(0);
                aiphoneRes.setJsonResponse(true);
                aiphoneRes.setBody("{\n" +
                        "    \"tool\": \"transfer_to_agent\",\n" +
                        "    \"arguments\": {}\n" +
                        "}");
                 */

                if(aiphoneRes.isJsonResponse()){
                    LlmToolRequest toolRequest = JSON.parseObject(
                            aiphoneRes.getBody(), LlmToolRequest.class);
                    if(toolRequest.getTool().equals(LlmToolRequest.TRANSFER_TO_AGENT)) {
                        transferToAgent = true;
                        InboundDetail callDetailNew = new InboundDetail(
                                UuidGenerator.GetOneUuid(),
                                callDetail.getCaller(),
                                callDetail.getCallee(),
                                System.currentTimeMillis(),
                                callDetail.getUuid(),
                                callDetail.getWavFile(),
                                callDetail.getGroupId(),
                                String.valueOf(callDetail.getRemoteVideoPort())
                        );
                        logger.info("{} stop_asr process.", uuid);
                        chatRobot.sendTtsRequest("请稍后，现在为您转接专家坐席。");
                        // stop_asr 的顺序很重要，需要放在播放tts之后，否则不起作用；会被uuid_break清空指令;
                        EslConnectionUtil.sendExecuteCommand("stop_asr", "", uuid);
                        acquire(5000);
                        // wait for tips playback finished

                        this.processFsMsg(this.generateHangupEvent("transferToAgent"));
                        AppContextProvider.getBean(InboundDetailService.class).insertInbound(callDetailNew);
                        TransferToAgent.transfer(callDetailNew);
                        return;
                    }

                    if(toolRequest.getTool().equals(LlmToolRequest.HANGUP)) {
                        chatRobot.sendTtsRequest("欢迎您再次来电咨询，祝您生活愉快，再见!");
                        acquire(6000);
                        hangupAndCloseConn();
                        return;
                    }

                    if(toolRequest.getTool().equals(LlmToolRequest.KB_QUERY)) {
                        chatRobot.sendTtsRequest("正在为您查询，请稍等。");
                        acquire(7000);
                        // wait for tips playback finished

                        String cat = toolRequest.getArguments().getString("key");
                        LlmKb kb = AppContextProvider.getBean(SysService.class).getKbContentByCat(cat);
                        String response =  aiphoneRes.getBody() + " 的查询结果如下:";
                        if(null != kb){
                            response += kb.getContent();
                        }else{
                            response += " 没有查询到相关信息。";
                        }
                        interactWithRobot(response);
                        return;
                    }
                }

            } catch (Throwable e) {
                logger.error("{} talkWithLargeModel error! {} {} ",
                        getTraceId(), e.toString(), CommonUtils.getStackTraceString(e.getStackTrace())
                );
                hangupAndCloseConn();
                return;
            }


        if(aiphoneRes.getIfcan_interrupt() == 1) {
            interactiveParam.setAllowInterrupt(1);
            logger.info("{} allowSpeechInterrupt={}", getTraceId(), 1);
        }

        // 设置 moveCode
        // interactiveParam.getRobotParam().setCallCode(responseObj.getString("callCode")) ;
        if (!interactiveParam.checkInHangupState()) {
            // code状态码000000代表返回成功的结果
            if (aiphoneRes.getStatus_code() == 1) {
                if (aiphoneRes.getClose_phone() == 1) {
                    logger.info(getTraceId() + " hangup signal is detected. ");
                    interactiveParam.setInHangUpState(true);
                     recvHangupSignal = true;
                }else{
                    waitForCustomerSpeakEx();
                }
            }
        }
    }

    private void  waitAndDetectSpeaking(long tinyDelay){
        if (isHangup) {
            return;
        }
        if(tinyDelay > 10L) {
            acquire(tinyDelay);
        }
        if (interactiveParam.checkInSpeaking()){
            logger.info("{} Speaking is detected, Wait for customer to finish speaking. Timeout: {} ",
                    getTraceId(),
                    maxWaitTimeMills
            );
            acquire(maxWaitTimeMills);
        }
    }

    /**
     * 用户在机器播放完成后保持静音，调用方法
     */
    private void waitForCustomerSpeakEx() {
        if (isHangup) {
            return;
        }

        logger.info("{} enter into waitForCustomerSpeak ...", getTraceId());

        // 流式tts播报时间不要超过181秒;
        acquire(181000);
        if (isHangup) {
            return;
        }

        if(!recvPlayBackEndEvent) {
            logger.info("{} robot speech interrupt detected. ", getTraceId());
        }else{
            logger.info("{} robot speech playback finished. ", getTraceId());
        }

        if(getAsrModelType().equalsIgnoreCase(ASR_TYPE_WEBSOCKET)) {
            resumeAsr();
        }
        if (getAsrModelType().equalsIgnoreCase(ASR_TYPE_MRCP)) {
            startMrcp();
        }

        long startWaitTimeMills = System.currentTimeMillis();

        acquire(6000);
        if (isHangup) {
            return;
        }

        // 6秒内有讲话，则继续等待下去;
        waitAndDetectSpeaking(10);
        logger.info(getTraceId() + " Robot main thread has woken up.");


        if (!interactiveParam.checkInSpeaking() && !getAllowInterrupt() ) {
            // 前面的流程都正常，客户讲话有中间结果，且有最终的vad结果;
            // 根据vad结果产生不同的时间段，计算不同的应继续等待时间;
            long waitMills = calcWaitSecsDuration6Secs();
            if (waitMills > 100L) {
                logger.info("{} Wait another {} milliseconds to ensure the customer is finished speaking. ",
                        getTraceId(),
                        waitMills
                );
                acquire(waitMills);
                waitAndDetectSpeaking(10);
            }
        }

        if (!isHangup) {
            if (!recvPlayBackEndEvent) {
                if (getAllowInterrupt()) {
                    logger.info("{} Detected customer interruption during robot's speech playback,  continue to wait for the customer to complete the speech {} ms",
                            getTraceId(),
                            interruptWaitMills
                    );
                    // 检测到客户打断之后，继续等待看客户是否有继续讲话，如果有则继续等待；
                    acquire(interruptWaitMills);
                    if (interactiveParam.checkInSpeaking()) {
                        acquire(maxWaitTimeMills);
                    }
                    interruptRobotHappened = true;
                }
            }

            //如果没有接收到asr识别结果，则延迟下，继续等待0.5秒钟：
            if (asrResultEx.size() == 0) {
                acquire(500);
            }

            if (isHangup) {
                return;
            }

            if (asrResultEx.size() == 0) {
                logger.info("{} No asr result got: NO_VOICE ", getTraceId());
            } else {
                calleeSpeakNumber.incrementAndGet();
            }
            interactRounds.incrementAndGet();

            int muteTimeLong = (int) (System.currentTimeMillis() - startWaitTimeMills);
            logger.info("{} The time spent waiting for the customer to finish speaking is {} ms.",getTraceId(), muteTimeLong);
            interactWithRobot("");
        }
    }


    /**
     * 显示本通电话的 No_Voice轮次，打印 语音识别连接是否成功信息;
     */
    private void displayNoVoiceNum() {
        String tips = "";
        if (calleeSpeakNumber.get() == 0) {
            tips = "No speech was detected all over the session.";
        }
        logger.info("{} {} calleeSpeakNumber:{}, interactRounds:{}, NO_VOICE_NUMBER:{}, dropAsrCounter:{}, wavFile: {}, recordings file exists：{}",
                getTraceId(),
                tips,
                calleeSpeakNumber.get(),
                interactRounds.get(),
                interactRounds.get() - calleeSpeakNumber.get(),
                dropAsrCounter.get(),
                this.recordingsFileName,
                interactRounds.get() != 0 ? new java.io.File(this.recordingsFileName).exists() : "No talk interaction."
        );
    }

    /**
     * saveCdr
     */
    private void saveCdr(Map<String, String> headers) {
        String hangupCause = headers.get("Hangup-Cause");
        String sipCode = headers.get("variable_proto_specific_hangup_cause");
        logger.info("{} session is hangup, hangupCause={}, sipCode={}", getTraceId(), hangupCause, sipCode);
        callDetail.setExtnum("robot");
        callDetail.setOpnum("robot");
        callDetail.setHangupTime(System.currentTimeMillis());
        callDetail.setChatContent(chatRobot.getDialogues());
        long timeLen = System.currentTimeMillis() - callDetail.getAnsweredTime();
        callDetail.setTimeLen(timeLen);
        AcdSqlQueue.addToSqlQueue(callDetail);
    }

}

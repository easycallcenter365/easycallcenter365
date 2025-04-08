package com.telerobot.fs.service;

import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.dto.CallMonitorInfo;
import com.telerobot.fs.entity.pojo.AsrEntity;
import com.telerobot.fs.entity.pojo.AsrResult;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadPoolCreator;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.MessageHandlerEngine;
import com.telerobot.fs.wshandle.MessageHandlerEngineList;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.RespStatus;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@DependsOn({"fsEslStarter"})
public class AsrResultListener implements ApplicationListener<ApplicationReadyEvent>, IEslEventListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AsrResultListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if(Boolean.parseBoolean(SystemConfig.getValue("fs_call_asr_enabled", "false"))) {
            logger.warn("fs_call_asr_enabled=true, Realtime speech to text recognition result will be send to consumers.");
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            startApplication();
                        }
                    }
            ).start();
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            startDestroy();
                        }
                    }
            ).start();
        }else{
            logger.warn("fs_call_asr_enabled=false, realtime speech to text disabled.");
        }
    }

    /**
     * 启动asr实时结果监听
     */
    private void startApplication() {
        asrResultListenerThread = ThreadPoolCreator.create(10, "asrResultListenerThread", 12L, 1000);
        eslConnectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
        eslConnectionPool.getDefaultEslConn().addDefaultListener(this);
    }

    private void startDestroy() {
        int sleepMills = 7000;
        while (true) {
            Iterator<Map.Entry<String, AsrEntity>> entries = asrInfoContainer.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, AsrEntity> entry = entries.next();
                String callId = entry.getKey();
                AsrEntity asrEntity = entry.getValue();
                if (null != asrEntity) {
                    Long millsPassed = System.currentTimeMillis() - asrEntity.getHangupTime();
                    if ((asrEntity.getHangupTime() > 1L) && (millsPassed > 11000)) {
                        asrInfoContainer.remove(callId);
                    }
                }
            }
            ThreadUtil.sleep(sleepMills);
         }
    }

    private static ThreadPoolExecutor asrResultListenerThread = null;
    private static EslConnectionPool eslConnectionPool = null;

    @Override
    public void eventReceived(String addr, EslEvent event) {
        asrResultListenerThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    processFsMsg(event.getEventHeaders());
                } catch (Exception e) {
                    logger.error("{} processFsMsg error : {}", e.toString(),
                            CommonUtils.getStackTraceString(e.getStackTrace()));
                }
            }
        });
    }

    @Override
    public void backgroundJobResultReceived(String addr, EslEvent event) {
        logger.info("backgroundJobResultReceived : {} ", event.toString());
    }

    /**
     * 保存所有通话信息的容器
     */
    private static Map<String, AsrEntity> asrInfoContainer = new ConcurrentHashMap<>(1000);

    /**
     * 启动一个通话的asr识别流程; 同时启动对坐席和客户的语音识别
     */
    public static void startCallAsrProcess(CallMonitorInfo callMonitorInfo) {
        EslConnectionUtil.sendExecuteCommand("start_asr", "hello", callMonitorInfo.getUuid());
        EslConnectionUtil.sendExecuteCommand("start_asr", "hello", callMonitorInfo.getUuidAgent());

        String uuidAgent = callMonitorInfo.getUuidAgent();
        String uuidCustomer = callMonitorInfo.getUuid();
        AsrEntity asrEntity = new AsrEntity();
        asrEntity.setExtnum(callMonitorInfo.getExtNum());
        asrEntity.setStartTime(System.currentTimeMillis());
        asrEntity.setCustomerPhone(callMonitorInfo.getCustomerPhone());
        asrEntity.setUuidAgent(uuidAgent);
        asrEntity.setUuidCustomer(uuidCustomer);

        asrInfoContainer.put(uuidAgent, asrEntity);
        asrInfoContainer.put(uuidCustomer, asrEntity);
        sendAsrResultToAgent(callMonitorInfo.getExtNum(),
                RespStatus.ASR_PROCESS_STARTED, "ASR_PROCESS_STARTED", null
        );
    }

    private static void sendAsrResultToAgent(String extNum, int eventCode, String tips, Object body){
        MessageHandlerEngine engine =MessageHandlerEngineList.getInstance().
                getMsgHandlerEngineByExtNum(extNum);
        if(engine != null){
            MessageResponse response = new MessageResponse(eventCode, tips, body);
            engine.sendReplyToAgent(response);
        }
    }

    public void processFsMsg(Map<String, String> headers) {
        String eventName = headers.get("Event-Name");
        String uniqueId = headers.get("Unique-ID");
        AsrEntity asrEntity = asrInfoContainer.get(uniqueId);
        if (null == asrEntity) {
            return;
        }

        String extNum = asrEntity.getExtnum();
        if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
            asrEntity.setHangupTime(System.currentTimeMillis());
            int eventCode = RespStatus.ASR_PROCESS_END_AGENT;
            if(uniqueId.equalsIgnoreCase(asrEntity.getUuidCustomer())){
                eventCode = RespStatus.ASR_PROCESS_END_CUSTOMER;
            }
            sendAsrResultToAgent(extNum, eventCode, "ASR_PROCESS_END", null);

        } else if (EventNames.CUSTOM.equalsIgnoreCase(eventName)) {
            String eventSubClass = headers.get("Event-Subclass");
            String asrEventDetail = headers.get("ASR-Event-Detail");
            String asrEventTimeMs = headers.get("ASR-Event-Time-Ms");
            String text = headers.get("Detect-Speech-Result");
            if(!"AsrEvent".equalsIgnoreCase(eventSubClass)){
                return;
            }
            if(StringUtils.isNullOrEmpty(text)){
                return;
            }

            int eventCode = RespStatus.ASR_RESULT_GENERATE;
            int role = 2;
            if(uniqueId.equalsIgnoreCase(asrEntity.getUuidCustomer())){
                role = 1;
            }
            AsrResult result = new AsrResult();
            result.setRole(role);
            result.setVadTime(System.currentTimeMillis());
            result.setText(text);
            result.setVadId(UuidGenerator.GetOneUuid());

            if("Middle".equalsIgnoreCase(asrEventDetail)){
                result.setVadType(0);
            }else{
                result.setVadType(1);
            }
            if(!StringUtils.isNullOrEmpty(asrEventTimeMs)){
                result.setVadTime(Long.parseLong(asrEventTimeMs));
            }
            asrEntity.getAsrResults().add(result);
            sendAsrResultToAgent(extNum, eventCode, "ASR_RESULT_GENERATE", result);
        }
    }
}

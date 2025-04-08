package com.telerobot.fs.wshandle;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.utils.ThreadUtil;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import org.springframework.context.annotation.DependsOn;
import link.thingscloud.freeswitch.esl.EslConnectionDetail;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.FreeswitchNodeInfo;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@DependsOn({"appContextProvider", "myStartupRunner"})
public class FsEslStarter implements ApplicationListener<ApplicationReadyEvent> {
    protected final static Logger logger = LoggerFactory.getLogger(FsEslStarter.class);
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("try to create freeSwitch esl connection pool.");
        checkFreeswitchConnection();
        List<String> eventSubscriptions = new ArrayList<>();

        eventSubscriptions.add(EventNames.CHANNEL_HANGUP);
        eventSubscriptions.add(EventNames.CHANNEL_ANSWER);
        eventSubscriptions.add(EventNames.CHANNEL_PROGRESS_MEDIA);
        eventSubscriptions.add(EventNames.HEARTBEAT);
        eventSubscriptions.add(EventNames.BACKGROUND_JOB);
        eventSubscriptions.add(EventNames.DETECTED_SPEECH);
        eventSubscriptions.add(EventNames.CHANNEL_PARK);
        eventSubscriptions.add(EventNames.RECORD_START);
        eventSubscriptions.add(EventNames.RECORD_STOP);
        eventSubscriptions.add(EventNames.PLAYBACK_STOP);
        eventSubscriptions.add(EventNames.PLAYBACK_START);
        eventSubscriptions.add(EventNames.DTMF);
        eventSubscriptions.add("CUSTOM AsrEvent");
        eventSubscriptions.add("CUSTOM TtsEvent");

        EslConnectionDetail.setEventSubscriptions(eventSubscriptions);
        List<FreeswitchNodeInfo> nodeList = new ArrayList<>(8);
        String host = SystemConfig.getValue("event-socket-ip");
        int port = Integer.parseInt(SystemConfig.getValue("event-socket-port"));
        String pass = SystemConfig.getValue("event-socket-pass");
        int poolSize = Integer.parseInt(SystemConfig.getValue("event-socket-conn-pool-size"));
        FreeswitchNodeInfo nodeInfo = new FreeswitchNodeInfo();
        nodeInfo.setHost(host);
        nodeInfo.setPort(port);
        nodeInfo.setPass(pass);
        nodeInfo.setPoolSize(poolSize);
        nodeList.add(nodeInfo);
        EslConnectionUtil.initConnPool(nodeList);
    }

    private void checkFreeswitchConnection(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(21000);
                EslConnectionPool pool = EslConnectionUtil.getDefaultEslConnectionPool();
                if(pool == null){
                    logger.error("严重错误，无法连接到Freeswitch，请检查是否启动了.");
                    System.exit(1);
                }

            }
        }).start();
    }
}

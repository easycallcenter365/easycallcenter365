package com.telerobot.fs.wshandle.impl;

import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.WebsocketThreadPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CallIVR  {
    private static final Logger logger = LoggerFactory.getLogger(CallIVR.class);

    /**
     *  播放mp4视频文件;
     *  播放完毕后，语音播报: 重听请按键1，挂机请按键 0; 按键10秒超时自动挂机
     *  播放次数触达 maxPlayCount 后自动挂机;
     * @param uuid
     * @param filePath
     * @param maxPlayCount
     */
    public static void playMp4File(String phone, String uuid, String filePath, int maxPlayCount) {

        EslConnectionUtil.sendExecuteCommand( "playback", filePath, uuid);

        IEslEventListener listener = new IEslEventListener() {
            int playCounter = 1;
            private volatile boolean dtmfRecv = false;
            private volatile boolean playedFinished = false;
            // 播放提示音，询问是否继续播放;
            private String soundTips = "$${sounds_dir}/ivr/replay-video-tips.wav";
            private String getTraceId() {
                return uuid;
            }
            @Override
            public void eventReceived(String addr, EslEvent event) {
                Map<String, String> headers = event.getEventHeaders();
                String uniqueId = headers.get("Unique-ID");
                String eventName = headers.get("Event-Name");

                if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
                   logger.info("{} phone={} is hangup. ", getTraceId(), phone);

                }else if (EventNames.PLAYBACK_STOP.equalsIgnoreCase(eventName)) {

                    logger.info("{} recv PLAYBACK_STOP event. ", getTraceId());
                    String playBackFilePath = headers.get("Playback-File-Path");
                    if(playCounter < maxPlayCount && playBackFilePath.endsWith(".mp4")){
                        playedFinished = true;
                        dtmfRecv = false;
                        EslConnectionUtil.sendExecuteCommand( "playback", soundTips, uuid);
                        WebsocketThreadPool.addTask(new Runnable() {
                            @Override
                            public void run() {
                               // 设定一个定时器，超时10秒收取不到客户语音按键则挂机;
                                int counter = 0; //语音播报时长大概是4秒;
                                while (!dtmfRecv && counter < 30){
                                    ThreadUtil.sleep(500);
                                    counter ++;
                                }
                                if(!dtmfRecv){
                                    EslConnectionUtil.sendExecuteCommand( "hangup", "wait-for-dtmf-Timeout", uuid);
                                }
                            }
                        });
                    }

                    if(playCounter >= maxPlayCount){
                        EslConnectionUtil.sendExecuteCommand( "hangup", "play count exceed maxPlayCount.", uuid);
                    }

                }else if (EventNames.PLAYBACK_START.equalsIgnoreCase(eventName)) {
                    logger.info("{} recv PLAYBACK_START event.  uniqueId={}", getTraceId(), uniqueId);

                }else if (EventNames.DTMF.equalsIgnoreCase(eventName)) {
                    logger.info("{} recv DTMF event.  uniqueId={}", getTraceId(), uniqueId);
                    String digit = headers.get("DTMF-Digit");
                    if(!playedFinished){
                        logger.info("{} playMp4File is not done. Ignore dtmf digit .", getTraceId());
                        return;
                    }
                    if("1".equals(digit)){
                        dtmfRecv = true;
                        playedFinished = false;
                        playCounter ++;
                        EslConnectionUtil.sendExecuteCommand( "playback", filePath, uuid);
                    }else if("0".equals(digit)){
                        dtmfRecv = true;
                        EslConnectionUtil.sendExecuteCommand( "hangup", "Customer-Manually", uuid);
                    }else{
                        dtmfRecv = false;
                        EslConnectionUtil.sendExecuteCommand( "playback", soundTips, uuid);
                    }

                }
            }

            @Override
            public void backgroundJobResultReceived(String addr, EslEvent event) {
            }
        };

        EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().addListener(uuid, listener);
    }
}

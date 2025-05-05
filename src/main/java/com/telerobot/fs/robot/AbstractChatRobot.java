package com.telerobot.fs.robot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import okhttp3.OkHttpClient;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class AbstractChatRobot implements IChatRobot {

    protected final static Logger logger = LoggerFactory.getLogger(AbstractChatRobot.class);

    protected static final OkHttpClient CLIENT =  new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    protected String uuid;

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *  tts通道已关闭
     */
    protected volatile boolean ttsChannelClosed = true;

    protected String getTraceId(){
        return uuid;
    }

    protected List<JSONObject> llmRoundMessages = new ArrayList<>();
    protected int ttsTextLength = 0;
    protected ArrayBlockingQueue<String> ttsTextCache = new ArrayBlockingQueue<String>(2000);
    private final  String[] pauseFlags = new String[]{
            "？", "?",
            "，", ",",
            "；", ";",
            "。", ".",
            "、",
            "！", "!",
            "：", ":"
    };
    protected boolean checkPauseFlag(String input){
        String lastChar = input.substring(input.length() - 1);
        for (String flag : pauseFlags) {
            if(flag.equalsIgnoreCase(lastChar)){
                return true;
            }
        }
        return false;
    }
    protected void sendToTts() {
        StringBuilder tmpText = new StringBuilder("");
        while (ttsTextCache.peek() != null) {
            tmpText.append(ttsTextCache.poll());
        }
        String text = tmpText.toString();
        sendTtsRequest(text);
        ttsTextLength = 0;
    }

    @Override
    public void sendTtsRequest(String textParam){
        if(StringUtils.isEmpty(textParam)){
            return;
        }
        String text = textParam.replace(" ", "")
                .replace("\\", "")
                .replace("*", "")
                .replace("\n", "");
        String voiceSource =  SystemConfig.getValue("stream-tts-voice-source", "aliyuntts");
        String voiceCode =  SystemConfig.getValue("stream-tts-voice-name", "aixia");

        if(ttsChannelClosed) {
            EslConnectionUtil.sendSyncApiCommand("uuid_break", uuid + " all");
            EslConnectionUtil.sendExecuteCommand("speak", String.format("%s|%s|%s", voiceSource, voiceCode, text), uuid);
            ttsChannelClosed = false;
            logger.info("{} sendTtsRequest speak tts text {}", uuid, text);
        }else{
            EslConnectionUtil.sendExecuteCommand("aliyuntts_resume", text, uuid);
            logger.info("{} sendTtsRequest cosvtts_resume text {}", uuid, text);
        }
    }

    @Override
    public String getDialogues(){
        return JSON.toJSONString(llmRoundMessages);
    }

    @Override
    public void setTtsChannelState(boolean closed) {
       this.ttsChannelClosed = closed;
    }

    @Override
    public abstract LlmAiphoneRes talkWithAiAgent(String question);
}

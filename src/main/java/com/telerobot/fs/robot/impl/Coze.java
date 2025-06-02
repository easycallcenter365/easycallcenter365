package com.telerobot.fs.robot.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.JWTOAuthClient;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.robot.AbstractChatRobot;
import com.telerobot.fs.utils.CommonUtils;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import link.thingscloud.freeswitch.esl.util.CurrentTimeMillisClock;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Coze  extends AbstractChatRobot {

    private String conversationId = "";
    private static final String COZE_TOKEN_TYPE_PAT = "pat";
    private static final String COZE_TOKEN_TYPE_OAUTH = "oauth";
    private String token = "";
    private int expireTime = 0;

    private String getToken(){
        String cozeTokenType = SystemConfig.getValue("coze_token_type", "pat");
        if(COZE_TOKEN_TYPE_PAT.equalsIgnoreCase(cozeTokenType)){
            return SystemConfig.getValue("coze-pat-token");
        }

        String  cozeAPIBase = "https://api.coze.cn";
        String jwtOauthClientID = SystemConfig.getValue("coze_jwt_oauth_client_id");
        String jwtOauthPrivateKey = SystemConfig.getValue("coze_jwt_oauth_private_key");
        String jwtOauthPublicKeyID = SystemConfig.getValue("coze_jwt_oauth_public_key_id");
        JWTOAuthClient oauth = null;

        try {
            oauth = new JWTOAuthClient.JWTOAuthBuilder()
                            .clientID(jwtOauthClientID)
                            .privateKey(jwtOauthPrivateKey)
                            .publicKey(jwtOauthPublicKeyID)
                            .baseURL(cozeAPIBase).ttl(24 * 3600)
                            .build();
        } catch (Throwable e) {
            logger.error("{} coze getToken error: {} {} ", uuid, e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
            return "";
        }

        Long expiredMillsLeft = expireTime - (CurrentTimeMillisClock.now()/1000);
        //过期前1小时
        if (StringUtils.isEmpty(token) || expiredMillsLeft < 3600 || expireTime == 0) {
            synchronized (jwtOauthClientID.intern()) {
                if (StringUtils.isEmpty(token) || expiredMillsLeft < 3600 || expireTime == 0) {
                    OAuthToken aAuthToken = null;
                    try {
                        aAuthToken = oauth.getAccessToken();
                        expireTime = aAuthToken.getExpiresIn();
                        token = aAuthToken.getAccessToken();
                        logger.info("{} successfully getAccessToken={} , expireTime={} ",
                                uuid, aAuthToken.getAccessToken().substring(0, 30) + "***", aAuthToken.getExpiresIn());
                    } catch (Throwable e) {
                        logger.error("{} coze getAccessToken error: {} ", uuid, CommonUtils.getStackTraceString(e.getStackTrace()));
                        return "";
                    }
                }
            }
        }
       return token;
    }

    private  final Object waitHandle = new Object();
    private  void release(){
        synchronized (waitHandle) {
            waitHandle.notify();
        }
    }
    private  void acquire(){
        try {
            synchronized (waitHandle) {
                waitHandle.wait();
            }
        }
        catch (Throwable throwable){
            logger.error("{} acquire error: {} {} ", uuid, throwable.toString() ,
                    CommonUtils.getStackTraceString(throwable.getStackTrace()));
        }
    }

    @Override
    public LlmAiphoneRes talkWithAiAgent(String question) {
        LlmAiphoneRes aiphoneRes = new LlmAiphoneRes();
        aiphoneRes.setStatus_code(1);
        aiphoneRes.setClose_phone(0);
        aiphoneRes.setIfcan_interrupt(0);

        JSONObject userMessage1 = new JSONObject();
        userMessage1.put("role", "user");
        userMessage1.put("content", question);
        userMessage1.put("content_type", "text");
        llmRoundMessages.add(userMessage1);

        try {
            JSONObject response = sendStreamingRequest(aiphoneRes, question, getToken());
            llmRoundMessages.add(response);
        }catch (Throwable throwable) {
            logger.error("{} talkWithAiAgent error: {}", uuid, CommonUtils.getStackTraceString(throwable.getStackTrace()));
        }

        return aiphoneRes;
    }

    private JSONObject sendStreamingRequest(LlmAiphoneRes aiphoneRes, String question, String cozeToken){
        JSONObject finalResponse = new JSONObject();
        finalResponse.put("role", "assistant");

        TokenAuth authCli = new TokenAuth(cozeToken);
        // Init the Coze client through the access_token.
        CozeAPI coze =
                new CozeAPI.Builder()
                        .baseURL("https://api.coze.cn/v3/chat/")
                        .auth(authCli)
                        .readTimeout(10000)
                        .build();

        /*
         * Step one, create chat
         * Call the coze.chat().stream() method to create a chat. The create method is a streaming
         * chat and will return a Flowable ChatEvent. Developers should iterate the iterator to get
         * chat event and handle them.
         * */
        String botID =  SystemConfig.getValue("coze-bot-id");
        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(botID)
                        .userID(uuid)
                        .messages(Collections.singletonList(Message.buildUserQuestionText(question)))
                        .build();
        if(!StringUtils.isEmpty(conversationId)) {
            req.setConversationID(conversationId);
        }

        Flowable<ChatEvent> resp = coze.chat().stream(req);
        StringBuilder responseBuilder = new StringBuilder();
        AtomicBoolean jsonFormat = new AtomicBoolean(false);
        AtomicBoolean recvData = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        resp.subscribeOn(Schedulers.io())
                .subscribe(
                        event -> {
                            if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                                System.out.print(event.getMessage().getContent());

                                if(StringUtils.isEmpty(conversationId)){
                                    conversationId = event.getMessage().getConversationId();
                                    logger.info("{} coze chat conversation_id = {}", uuid, conversationId);
                                }

                                if (!recvData.get()) {
                                    recvData.set(true);
                                    long costTime = (System.currentTimeMillis() - startTime);
                                    logger.info("http request cost time : {} ms.", costTime);
                                    aiphoneRes.setCostTime(costTime);
                                }

                                String tmpText = event.getMessage().getContent().trim();

                                if (tmpText.startsWith("{") && !jsonFormat.get()) {
                                    logger.info("{} json response detected.", getTraceId());
                                    jsonFormat.set(true);
                                    aiphoneRes.setJsonResponse(true);
                                }

                                if (!StringUtils.isEmpty(tmpText) && !jsonFormat.get()) {
                                    ttsTextCache.add(tmpText);
                                    ttsTextLength += tmpText.length();
                                    // 积攒足够的字数之后，才发送给tts，避免播放异常;
                                    if (ttsTextLength >= 10 && checkPauseFlag(tmpText)) {
                                        sendToTts();
                                    }
                                }
                                responseBuilder.append(tmpText);
                            }
                            if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                               logger.info("{} Token usage count = {}." , uuid, event.getChat().getUsage().getTokenCount());
                            }
                        },
                        throwable -> {
                            System.err.println(": " + throwable.getMessage());
                            logger.info("{} coze error occurred {} {}", uuid, throwable.toString(),
                                    CommonUtils.getStackTraceString(throwable.getStackTrace()));
                            release();
                        },
                        () -> {
                            logger.info("{} coze talk done.", uuid);
                            release();
                        });

        acquire();
        coze.shutdownExecutor();

        String answer = responseBuilder.toString();
        logger.info("{} coze answer={}", this.uuid, answer);
        if(ttsTextLength > 0 && !jsonFormat.get()){
            sendToTts();
        }
        closeTts();

        finalResponse.put("content", answer);
        aiphoneRes.setBody(answer);
        return finalResponse;
    }
}

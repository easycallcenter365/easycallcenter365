package com.telerobot.fs.robot.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.robot.AbstractChatRobot;
import com.telerobot.fs.utils.CommonUtils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

public class MaxKB extends AbstractChatRobot {

    /**
     * MaxKB 相关的三个参数请在数据库中 cc_params 表中设置
     */
    private static final String API_KEY = SystemConfig.getValue("maxkb-api-key");
    private static  final  String Server_URL = SystemConfig.getValue("maxkb-server-url");
    private static final String MODEL_NAME =  SystemConfig.getValue("maxkb-model");
    private String chat_id = "";

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
            JSONObject response = sendStreamingRequest(aiphoneRes);
            llmRoundMessages.add(response);
        }catch (Throwable throwable) {
            logger.error("{} talkWith MaxKB error: {}", uuid, CommonUtils.getStackTraceString(throwable.getStackTrace()));
        }

        return aiphoneRes;
    }

    private  JSONObject sendStreamingRequest(LlmAiphoneRes aiphoneRes) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("stream", true);
        if (!StringUtils.isEmpty(chat_id)) {
            requestBody.put("chat_id", chat_id);
            logger.info("{} set MaxKB chat_id = {}", uuid, chat_id);
        }
        // enable stream output

        JSONArray messagesArray = new JSONArray();
        messagesArray.add(llmRoundMessages.get(llmRoundMessages.size() - 1));
        requestBody.put("messages", messagesArray);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toJSONString()
        );

        Request request = new Request.Builder()
                .url(Server_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        boolean recvData = false;
        boolean jsonFormat = false;
        long startTime = System.currentTimeMillis();
        logger.info("{} http request startTime: {} ms.", uuid, startTime);

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("MaxKB api error: http-code={}, msg={}, url={}",
                        response.code(),
                        response.message(),
                        Server_URL
                );
                throw new IOException("Unexpected code " + response);
            }

            BufferedSource source = response.body().source();
            StringBuilder responseBuilder = new StringBuilder();

            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    String jsonData = line.substring(6).trim(); // 去掉 "data: " 前缀
                    JSONObject jsonResponse = JSON.parseObject(jsonData);
                    JSONObject message = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta"); // 注意：流式响应中消息在 "delta" 字段中

                    if (message.containsKey("chat_id")) {
                        String chatID = message.getString("chat_id");
                        if (!StringUtils.isEmpty(chatID)) {
                            chat_id = chatID;
                            logger.info("{} successfully get MaxKB chat_id = {}", uuid, chat_id);
                        }
                    }

                    if (message.containsKey("content")) {
                        String speechContent = message.getString("content");
                        if(StringUtils.isEmpty(speechContent)){
                            continue;
                        }
                        if (!recvData) {
                            recvData = true;
                            long costTime = (System.currentTimeMillis() - startTime);
                            logger.info("http request cost time : {} ms.", costTime);
                            aiphoneRes.setCostTime(costTime);
                        }

                        if (!StringUtils.isEmpty(speechContent)) {
                            //  send to tts server
                            String tmpText = speechContent.trim().replace(" ", "").replace(" ", "");
                            if (tmpText.startsWith("{") && !jsonFormat) {
                                logger.info("{} json response detected.", getTraceId());
                                jsonFormat = true;
                                aiphoneRes.setJsonResponse(true);
                            }

                            if (!StringUtils.isEmpty(tmpText) && !jsonFormat) {
                                ttsTextCache.add(tmpText);
                                ttsTextLength += tmpText.length();
                                // 积攒足够的字数之后，才发送给tts，避免播放异常;
                                if (ttsTextLength >= 10 && checkPauseFlag(tmpText)) {
                                    sendToTts();
                                }
                            }
                            responseBuilder.append(tmpText);
                        }
                    }
                }
            }

            String answer = responseBuilder.toString();
            logger.info("{} recv MaxKB response, answer={}", this.uuid, answer);
            if(ttsTextLength > 0 && !jsonFormat){
                sendToTts();
            }
            closeTts();

            JSONObject finalResponse = new JSONObject();
            finalResponse.put("role", "assistant");
            finalResponse.put("content", answer);
            aiphoneRes.setBody(answer);
            return finalResponse;
        }
    }

}

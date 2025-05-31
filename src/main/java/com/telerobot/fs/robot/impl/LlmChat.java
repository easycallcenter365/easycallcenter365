package com.telerobot.fs.robot.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.robot.AbstractChatRobot;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.FileUtils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

public class LlmChat extends AbstractChatRobot {

    /**
     * 大模型底座相关的三个参数请在数据库中 cc_params 表中设置
     */
    private static final String API_KEY = SystemConfig.getValue("model-api-key");
    private static  final  String MODEL_URL = SystemConfig.getValue("model-url");
    private static final String MODEL_NAME =  SystemConfig.getValue("model-name");

    private volatile boolean firstRound = true;

    /**
     *  faq问题手册内容
     */
    private static String faq;

    /**
     *  大模型提示词内容
     */
    private static String llmTips;

    static {
        String modelFaqDir = SystemConfig.getValue("model-faq-dir", "/home/call-center/kb/");
        String modelFaqPath = modelFaqDir + "faq.txt";
        faq = FileUtils.ReadFile(modelFaqPath, "utf-8");
        if(StringUtils.isEmpty(faq)){
            logger.error("cant not read file model-faq-path = {} , check whether the file exists!", modelFaqPath);
            System.exit(0);
        }
        logger.info("****** faq.txt file content: {}", faq);

        String llmTipsPath = modelFaqDir + "llmTips.txt";
        llmTips = FileUtils.ReadFile(llmTipsPath, "utf-8");
        if(StringUtils.isEmpty(llmTips)){
            logger.error("cant not read file  llmTipsPath = {} ，check whether the file exists!", llmTipsPath);
            System.exit(0);
        }
        logger.info("****** llmTips content: {}", llmTips);
    }

    @Override
    public LlmAiphoneRes  talkWithAiAgent(String question) {
        LlmAiphoneRes aiphoneRes = new  LlmAiphoneRes();
        aiphoneRes.setStatus_code(1);
        aiphoneRes.setClose_phone(0);
        aiphoneRes.setIfcan_interrupt(0);
        if(firstRound) {
            firstRound = false;
            JSONObject userMessage0 = new JSONObject();
            userMessage0.put("role", "system");
            String tips = llmTips + "\n" + faq;
            userMessage0.put("content", tips);
            llmRoundMessages.add(userMessage0);

            JSONObject userMessage1 = new JSONObject();
            userMessage1.put("role", "system");
            String openingRemarks = SystemConfig.getValue("llm-chat-opening-remarks", "电话已接通。请直播开场白，不超过30字");
            userMessage1.put("content", openingRemarks);
            llmRoundMessages.add(userMessage1);
        }else{
            JSONObject userMessage1 = new JSONObject();
            userMessage1.put("role", "user");
            userMessage1.put("content", question);
            llmRoundMessages.add(userMessage1);
        }

        try {
            JSONObject response = sendStreamingRequest(aiphoneRes, llmRoundMessages);
            llmRoundMessages.add(response);
        }catch (Throwable throwable){
            logger.error("{} talkWithAiAgent error: {}", uuid, CommonUtils.getStackTraceString(throwable.getStackTrace()));
        }

        return aiphoneRes;
    }



    private  JSONObject sendStreamingRequest(LlmAiphoneRes aiphoneRes, List<JSONObject> messages) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("stream", true);
        // enable stream output


        JSONArray messagesArray = new JSONArray();
        messagesArray.addAll(messages);
        requestBody.put("messages", messagesArray);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toJSONString()
        );

        Request request = new Request.Builder()
                .url(MODEL_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        boolean recvData = false;
        boolean jsonFormat = false;
        long startTime = System.currentTimeMillis();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Model api error: http-code={}, msg={}, url={}",
                        response.code(),
                        response.message(),
                        MODEL_URL
                );
                throw new IOException("Unexpected code " + response);
            }

            BufferedSource source = response.body().source();
            StringBuilder responseBuilder = new StringBuilder();

            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    String jsonData = line.substring(6).trim(); // 去掉 "data: " 前缀
                    if (jsonData.equals("[DONE]")) {
                        break; // 流式响应结束
                    }

                    JSONObject jsonResponse = JSON.parseObject(jsonData);
                    JSONObject message = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta"); // 注意：流式响应中消息在 "delta" 字段中

                    if (message.containsKey("content")) {
                        String speechContent = message.getString("content");
                        logger.info("{} speechContent: {}", getTraceId(), speechContent);

                        if (!recvData) {
                            recvData = true;
                            long costTime = (System.currentTimeMillis() - startTime);
                            logger.info("http request cost time : {} ms.", costTime);
                            aiphoneRes.setCostTime(costTime);
                        }

                        if (!StringUtils.isEmpty(speechContent)) {
                            //  send to tts server
                            if (speechContent.startsWith("{") && !jsonFormat) {
                                logger.info("{} json response detected.", getTraceId());
                                jsonFormat = true;
                                aiphoneRes.setJsonResponse(true);
                            }

                            if (!StringUtils.isEmpty(speechContent) && !jsonFormat) {
                                ttsTextCache.add(speechContent);
                                ttsTextLength += speechContent.length();
                                // 积攒足够的字数之后，才发送给tts，避免播放异常;
                                if (ttsTextLength >= 10 && checkPauseFlag(speechContent)) {
                                    sendToTts();
                                }
                            }
                            responseBuilder.append(speechContent);
                        }
                    }
                }
            }

            String answer = responseBuilder.toString();
            logger.info("{} recv llm response end flag. answer={}", this.uuid, answer);
            if(ttsTextLength > 0 && !jsonFormat){
                sendToTts();
            }

            JSONObject finalResponse = new JSONObject();
            finalResponse.put("role", "assistant");
            finalResponse.put("content", answer);
            aiphoneRes.setBody(answer);
            return finalResponse;
        }
    }
}

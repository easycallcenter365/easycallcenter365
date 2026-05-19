package com.telerobot.fs.robot.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.entity.dto.llm.LlmAccount;
import com.telerobot.fs.entity.po.HangupCause;
import com.telerobot.fs.entity.pojo.LlmToolRequest;
import com.telerobot.fs.robot.AbstractChatRobot;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.List;

public class GeminiChat extends AbstractChatRobot {

    @Override
    public LlmAiphoneRes  talkWithAiAgent(String question, Boolean... withKbResponse) {
        LlmAiphoneRes aiphoneRes = new LlmAiphoneRes();
        aiphoneRes.setStatus_code(1);
        aiphoneRes.setClose_phone(0);
        aiphoneRes.setIfcan_interrupt(0);
        if (firstRound) {
            firstRound = false;

            String llmTips = ((LlmAccount) getAccount()).getLlmTips();
            int catId = llmAccountInfo.kbCatId;
            String topicsVar = "${kbTopicList}";
            if(catId != -1) {
                if (llmTips.contains(topicsVar)) {
                    List<LlmKb> kbList = AppContextProvider.getBean(SysService.class).getKbListByCatId(catId);
                    StringBuilder topics = new StringBuilder();
                    for (LlmKb llmKb : kbList) {
                        topics.append("*").append(llmKb.getTitle()).append("\r\n");
                    }
                    topics.append("\r\n");
                    llmTips = llmTips.replace(topicsVar, topics.toString());
                    logger.info("{} topic list: {}", uuid, topics.toString());
                } else {
                    logger.warn("{} {} tag not found, the knowledge base function will be unavailable. ", uuid, topicsVar);
                }
            }else {
                logger.warn("{} The current model {} doesn’t have a knowledge base linked to it. ", uuid, ((LlmAccount) getAccount()).getModelName());
            }

            String tips = llmTips  + "\r\n\r\n" + ((LlmAccount) getAccount()).getFaqContext();
            JSONObject bizJson = new JSONObject();
            if (null != callDetail && null != callDetail.getOutboundPhoneInfo() && StringUtils.isNotBlank(callDetail.getOutboundPhoneInfo().getBizJson())) {
                tips += "\n bizJson:" + callDetail.getOutboundPhoneInfo().getBizJson();
                bizJson = JSONObject.parseObject(callDetail.getOutboundPhoneInfo().getBizJson());
            }
            addDialogue(ROLE_SYSTEM, tips);

            String openingRemarks = replaceParams(llmAccountInfo.openingRemarks, bizJson);

            addDialogue(ROLE_ASSISTANT, openingRemarks);

            ttsTextCache.add(openingRemarks);
            sendToTts();
            closeTts();

            aiphoneRes.setBody(openingRemarks);
            return aiphoneRes;
        } else {
            if (withKbResponse.length > 0 && !withKbResponse[0]) {
                if (!StringUtils.isEmpty(question)) {
                    addDialogue(ROLE_USER, question);
                } else {
                    addDialogue(ROLE_USER, "NO_VOICE");

                    String noVoiceTips = llmAccountInfo.customerNoVoiceTips;
                    addDialogue(ROLE_ASSISTANT, noVoiceTips);

                    ttsTextCache.add(noVoiceTips);
                    sendToTts();
                    closeTts();

                    aiphoneRes.setBody(noVoiceTips);
                    return aiphoneRes;
                }
            }

            try {
                JSONObject response = sendStreamingRequest(aiphoneRes, llmRoundMessages);
                if (null != response) {
                    llmRoundMessages.add(response);
                } else {
                    aiphoneRes.setStatus_code(0);
                }
            } catch (Throwable throwable) {
                aiphoneRes.setStatus_code(0);
                logger.error("{} talkWithAiAgent error: {} \n {}", uuid, throwable.toString(), CommonUtils.getStackTraceString(throwable.getStackTrace()));
            }
            return aiphoneRes;
        }
    }


    private  JSONObject sendStreamingRequest(LlmAiphoneRes aiphoneRes, List<JSONObject> messages) throws IOException {

//        curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent" \
//        -H "x-goog-api-key: your-api-key" \
//        -H 'Content-Type: application/json' \
//        -X POST \
//        -d '{
//        "systemInstruction": {
//            "parts": [{
//                "text": "You are a professional Java programming assistant. Answer concisely and clearly."
//            }]
//        },
//        "contents": [{
//            "role": "user",
//                    "parts": [{
//                "text": "What is multithreading?"
//            }]
//        }],
//        "generationConfig": {
//            "temperature": 0.7,
//                    "maxOutputTokens": 2048
//        }
//    }'

        String baseUrl = getAccount().serverUrl;
        String model = ((LlmAccount)getAccount()).getModelName();
        String apiKey = ((LlmAccount)getAccount()).getApiKey();
        String url = String.format("%s/v1beta/models/%s:generateContent",
                baseUrl, model);

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject systemInstruction = new JSONObject();
        for (JSONObject _msg: messages) {
            if (ROLE_SYSTEM.equals(_msg.getString("role"))) {
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", _msg.getString("content"));
                parts.add(part);
                systemInstruction.put("parts", parts);
            } else {
                JSONObject _msgObj = new JSONObject();
                _msgObj.put("role", _msg.getString("role"));
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", _msg.getString("content"));
                parts.add(part);
                _msgObj.put("parts", parts);
                contents.add(_msgObj);
            }
        }
        requestBody.put("systemInstruction", systemInstruction);
        requestBody.put("contents", contents);
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);
        generationConfig.put("topP", 0.9);
        requestBody.put("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toJSONString()
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .build();

        boolean recvData = false;
        long startTime = System.currentTimeMillis();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Model api error: http-code={}, msg={}, url={}",
                        response.code(),
                        response.message(),
                        url
                );
                if(response.code() == HttpStatus.SC_UNAUTHORIZED) {
                   CommonUtils.setHangupCauseDetail(
                           callDetail,
                           HangupCause.LLM_API_KEY_INCORRECT,
                           "http-status-code=" + response.code()
                   );
                  CommonUtils.hangupCallSession(uuid,  HangupCause.LLM_API_KEY_INCORRECT.getCode());
                  logger.error("response body:{}", response.body().string());
                  return null;
                }else{
                    CommonUtils.setHangupCauseDetail(
                            callDetail,
                            HangupCause.LLM_API_SERVER_ERROR,
                            "http-status-code=" + response.code()
                    );
                }

                if(response.code() == HttpStatus.SC_UNAUTHORIZED || response.code() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    throw new IOException("Unexpected code " + response);
                }else{
                    logger.error("response body:{}", response.body().string());
                    return null;
                }
            }

            BufferedSource source = response.body().source();
            StringBuilder responseBuilder = new StringBuilder();

//            data: {"candidates": [{"content": {"role": "model", "parts": [{"text": "你好"}]}, "index": 0}]}
//
//            data: {"candidates": [{"content": {"role": "model", "parts": [{"text": "！我是"}]}, "index": 0}]}
//
//            data: {"candidates": [{"content": {"role": "model", "parts": [{"text": " Gemini，很高兴"}]}, "index": 0}]}
//
//            data: {"candidates": [{"content": {"role": "model", "parts": [{"text": "为你服务。"}]}, "index": 0}]}
//
//            data: {"candidates": [{"content": {"role": "model", "parts": [{"text": ""}]}, "finishReason": "STOP", "index": 0}], "usageMetadata": {"promptTokenCount": 5, "candidatesTokenCount": 12, "totalTokenCount": 17}}

            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                try {
                    logger.info("{}-->{}", uuid, line);
                    if (line != null && line.startsWith("data: ")) {
                        String jsonData = line.substring(5).trim(); // 去掉 "data: " 前缀
                        if (jsonData.equals("[DONE]")) {
                            break; // 流式响应结束
                        }

                        JSONObject jsonResponse = JSON.parseObject(jsonData);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (null != candidates && candidates.size() > 0) {
                            String finishReason = candidates.getJSONObject(0).getString("finishReason");
                            if ("STOP".equals(finishReason) || "MAX_TOKENS".equals(finishReason)) {
                                break;
                            }
                            JSONObject message = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray parts = message.getJSONArray("parts");
                            String speechContent = "";
                            for (int i = 0; i < parts.size(); i++) {
                                speechContent += parts.getJSONObject(i).getString("text");
                            }

                            if (!StringUtils.isEmpty(speechContent)) {

                                if (speechContent.contains(LlmToolRequest.TRANSFER_TO_AGENT)) {
                                    aiphoneRes.setTransferToAgent(1);
                                    logger.info("{} `TRANSFER_TO_AGENT` command detected. ", getTraceId());
                                }

                                if (speechContent.contains(LlmToolRequest.HANGUP)) {
                                    aiphoneRes.setClose_phone(1);
                                    logger.info("{} `HANGUP` command detected. ", getTraceId());
                                }

                                if (!StringUtils.isEmpty(speechContent)) {
                                    speechContent = speechContent.replace(LlmToolRequest.TRANSFER_TO_AGENT,"")
                                            .replace(LlmToolRequest.HANGUP,"")
                                            .replace("`","");
                                    ttsTextCache.add(speechContent);
                                    ttsTextLength += speechContent.length();
                                    // 积攒足够的字数之后，才发送给tts，避免播放异常;
                                    if (ttsTextLength >= 5 && checkPauseFlag(speechContent)) {
                                        sendToTts();

                                        if (!recvData) {
                                            recvData = true;
                                            long costTime = (System.currentTimeMillis() - startTime);
                                            logger.info("http request cost time : {} ms.", costTime);
                                            aiphoneRes.setCostTime(costTime);
                                        }
                                    }
                                }
                                responseBuilder.append(speechContent);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.info("{}-->{}", uuid, line);
                    logger.error("parse llm response error：{}", ExceptionUtils.getStackTrace(e));
                }
            }

            String answer = responseBuilder.toString();
            logger.info("{} recv llm response end flag. answer={}", this.uuid, answer);
            if(ttsTextLength > 0){
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

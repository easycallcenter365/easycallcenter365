package com.telerobot.fs.entity.pojo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;

public class LlmToolRequest {

    public static final String TRANSFER_TO_AGENT = "transfer_to_agent";

    public static final String KB_QUERY = "kb_query";

    private String tool;
    private JSONObject arguments;

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public JSONObject getArguments() {
        return arguments;
    }

    public void setArguments(JSONObject arguments) {
        this.arguments = arguments;
    }

    public static void main(String[] args) {
        String json = "{\n" +
                "    \"tool\": \"transfer_to_agent\",\n" +
                "    \"arguments\": {  \"key\": \"计算机科学与技术\" }\n" +
                "}";
        LlmToolRequest request = JSON.parseObject(json, LlmToolRequest.class);
        System.out.println(request.tool + ";" + request.arguments.getString("key"));
    }
}

package com.telerobot.fs.controller;

import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.FileUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  捷通华生tts测试接口;
 */
@Scope("request")
@Controller
public class TtsTestController {

    private static final Logger logger = LoggerFactory.getLogger(TtsTestController.class);

    protected static final OkHttpClient CLIENT =  new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    @RequestMapping("/tts_test")
    @ResponseBody
    public String TtsTest(HttpServletRequest httpRequest, Map<String,Object> model)  {

        String text = httpRequest.getParameter("text");
        String path = httpRequest.getParameter("path");
        String url = "http://192.168.11.7:24100/v10/tts/synth/cn_xinwenhan_common/short_text";

        if(httpRequest.getParameter("url") != null){
            url = httpRequest.getParameter("url");
        }

        String json = "{\"config\":{\"format\":\"wav\",\"sampleRate\":16000,\"pitch\":0,\"volume\":50,\"speed\":0," +
                "\"digitMode\":0,\"engMode\":0,\"puncMode\":false,\"soundEffect\":0,\"useS3ML\":false,\"useVTML\":false," +
                "\"resultType\":\"BODY\"},\"text\":\""+ text +"\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                json
        );

        boolean success = false;
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type","application/json")
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {

            if(response.code() == 200) {
                // 保存wav语音文件到指定路径下
                success = FileUtil.writeToLocal(path, response.body().bytes());
            }else{
                logger.error("tts error: {}", response.body().string());
            }

        } catch (IOException e) {
            logger.error("tts error: {} {} ", e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
        }

        return success ?  "success" : "failed";
    }

}

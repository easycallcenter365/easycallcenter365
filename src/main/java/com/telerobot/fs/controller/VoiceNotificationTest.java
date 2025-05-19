package com.telerobot.fs.controller;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import okhttp3.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/VoiceNotificationTest")
public class VoiceNotificationTest {

    protected static final OkHttpClient CLIENT =  new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    @GetMapping("/start")
    @org.springframework.web.bind.annotation.ResponseBody
    public static String test(HttpServletRequest httpRequest) {
        String url = httpRequest.getParameter("url");
        String phone = httpRequest.getParameter("phone");
        String token =  httpRequest.getParameter("token");
        if(!SystemConfig.getValue("call-center-api-token", "").equals(token)){
            return "Forbidden.";
        }
        String json = " {\n" +
                "             \"wav_base_url\": \""+ url +"\",\n" +
                "             \"gateway_name\" : \"MRWG\",\n" +
                "             \"data\": [\n" +
                "                 {\"mobile\": \""+ phone +"\", \"wav\": \"\" },\n" +
                "                 {\"mobile\": \""+ phone +"\", \"wav\": \"\" }\n" +
                "             ]\n" +
                "         }";
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                json
        );

        Request request = new Request.Builder()
                .url("http://127.0.0.1:"+ AppContextProvider.getEnvConfig("server.port") +"/call-center/VoiceNotification/start")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {

               String resp =                 response.body().string();
               System.out.println(resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success.";
    }
}

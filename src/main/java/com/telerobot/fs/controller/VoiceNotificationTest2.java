package com.telerobot.fs.controller;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VoiceNotificationTest2 {

    protected static final OkHttpClient CLIENT =  new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {


        Request request = new Request.Builder()
                .url("http://127.0.0.1:8880/call-center/VoiceNotification/query?batch_id=2505142154420110001")
                .addHeader("Authorization", "Bearer sk-35ae192b2199b949547a581eb2ab1e72")
                .build();



        try (Response response = CLIENT.newCall(request).execute()) {

               String resp =                 response.body().string();
               System.out.println(resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

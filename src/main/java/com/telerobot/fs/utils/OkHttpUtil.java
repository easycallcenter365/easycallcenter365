package com.telerobot.fs.utils;

import okhttp3.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {
    private static  OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_PNG = MediaType
            .parse("image/png;charset=utf-8");
    private static final MediaType MEDIA_TYPE_MARKDOWN = MediaType
            .parse("text/x-markdown; charset=utf-8");
     static{
        client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5L, TimeUnit.MINUTES))
                .build();
     }
 
    /**
     * 不会开启异步线程。
     *
     * @param request
     * @return
     * @throws IOException
     */
    public static Response execute(Request request) throws IOException {
        return client.newCall(request).execute();
    }
 
    /**
     * 开启异步线程访问网络
     *
     * @param request
     * @param responseCallback
     */
    public static void enqueue(Request request, Callback responseCallback) {
        client.newCall(request).enqueue(responseCallback);
    }
 
    /**
     * 根据url地址获取数据
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String doGetHttpRequest(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        return response.body().string();
    }
 
    /**
     * 根据url地址和json数据获取数据
     *
     * @param url
     * @param json
     * @return
     * @throws IOException
     */
    public static String doPostHttpRequest(String url, String json)
            throws IOException {
        Request request = new Request.Builder().url(url)
                .post(RequestBody.create(JSON, json)).build();
 
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
 
            throw new IOException("Unexpected code " + response);
        }
        return response.body().string();
    }
 
    /**
     * 根据url地址和json数据获取数据
     *
     * @param url
     * @param json
     * @return
     * @throws IOException
     */
    public static String sendJsonRequest(String url, String json, Map<String,String> requestHeaders)
            throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        builder.addHeader("content-type", "application/json");
        Set<Map.Entry<String, String>> set = requestHeaders.entrySet();
        Iterator<Map.Entry<String, String>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> current = it.next();
            builder.addHeader(current.getKey(), current.getValue());
        }
        Request request = builder.build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        return response.body().string();
    }
 

}

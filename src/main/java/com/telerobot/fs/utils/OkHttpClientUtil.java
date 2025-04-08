package com.telerobot.fs.utils;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class OkHttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(OkHttpClientUtil.class);

    private static OkHttpClient httpClient;

    static{
        int httpConnPoolThreadNum = 10;
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(httpConnPoolThreadNum, 10L, TimeUnit.MINUTES))
                .build();
        logger.info("  OkHttpClient, The threads number of conn-pool: {}", httpConnPoolThreadNum);
    }

    /**
     *  请求远程api接口; (仅限json请求格式)
     * @param url  api接口地址
     * @param json  请求参数
     * @return
     */
    public static String curl(String url, String json, String method){
        Request.Builder builder= null;
        if("post".equals(method)){
             RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), json);
            builder = new Request.Builder()
                    .post(requestBody)
                    .url(url);
        }else{
            builder = new Request.Builder()
                    .get()
                    .url(url);
        }

        Request request = builder.build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
        } catch (Exception e) {
            logger.error(String.format("curl error, url: %s, request-json: %s, exception:%s",
                    url, json, e.toString()
            ));
            return "";
        }

        String responseStr = "";
        try {
            responseStr =  response.body().string();
        } catch (Exception e) {
            logger.error(String.format("curl error, url: %s, request-json: %s, exception:%s",
                    url, json, e.toString()
            ));
            return "";
        }
        int httpStatus = response.code();
        if(httpStatus != 200) {
            logger.error(String.format("curl error, url: %s, request-json: %s, httpStatus:%d, response:%s",
                    url, json, httpStatus, responseStr
            ));
        }
        return (httpStatus == 200) ?  responseStr : "";
    }

    /**
     *  请求远程api接口;
     * @param url  api接口地址
     * @param data  请求表单参数
     * @return
     */
    public static String postCdr(String url, String data){

        RequestBody requestBody = new FormBody.Builder().add("cdr", data).build();;
        Request.Builder  builder = new Request.Builder()
                    .post(requestBody)
                    .url(url);

        Request request = builder.build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
        } catch (Exception e) {
            logger.error(String.format("post error, url: %s, request-json: %s, exception:%s",
                    url, data, e.toString()
            ));
            return "";
        }

        String responseStr = "";
        try {
            responseStr =  response.body().string();
        } catch (Exception e) {
            logger.error(String.format("post error, url: %s, request-json: %s, exception:%s",
                    url, data, e.toString()
            ));
            return "";
        }
        int httpStatus = response.code();
        if(httpStatus != 200) {
            logger.error(String.format("post error, url: %s, request-json: %s, httpStatus:%d, response:%s",
                    url, data, httpStatus, responseStr
            ));
        }
        return (httpStatus == 200) ?  responseStr : "";
    }


    public static void main(String[] args) {
        String  url = "http://192.168.66.82:30202/api/collcall/coll/caseDetails/savePhoneCallRecord";
        String  json = "{\"answerTime\":1601203259000,\"callee\":\"17856923720\",\"caller\":\"1719\",\"caseNo\":\"SU2020061400013\",\"customerFirstHangup\":1,\"endTime\":1601203264000,\"extNum\":\"1719\",\"fullRecordPath\":\"1719_17856923720_uuid-20200927184053-1719.wav\",\"hangupCause\":\"NORMAL_CLEARING\",\"id\":\"202009271841041000001\",\"opNum\":\"admin\",\"projectId\":\"robotv4\",\"record_type\":\".wav\",\"savedCdr\":0,\"startTime\":1601203254000,\"timeLen\":10,\"uuid\":\"uuid-20200927184053-1719\",\"validTimeLen\":5}";
        String response =  OkHttpClientUtil.curl(url, json, "post");
        System.out.println(response);
    }

}

package com.telerobot.fs.global;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.po.CdrDetail;
import com.telerobot.fs.entity.po.CdrEntity;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.OkHttpClientUtil;
import com.telerobot.fs.utils.StringUtils;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

@Component
@DependsOn("appContextProvider")
public class CdrPush implements ApplicationListener<ApplicationReadyEvent> {

    private static Logger logger =  LoggerFactory.getLogger(CdrPush.class);
    private static Semaphore semaphore = new Semaphore(9999);
    private static ArrayBlockingQueue<CdrDetail> cdrQueue = new ArrayBlockingQueue<>(9999);


    public static boolean addCdrToQueue(CdrDetail cdr){
         if(cdrQueue.add(cdr)){
             semaphore.release(1);
             return true;
         }
         return  false;
    }

    private void postCdr(CdrDetail cdr){
        try {
            String url = SystemConfig.getValue("post_cdr_url");
            if (StringUtils.isNullOrEmpty(url)) {
                logger.error("没有配置业务系统接收话单的参数 post_cdr_url.");
                return;
            }
            String cdrData = JSON.toJSONString(cdr);
            String response = OkHttpClientUtil.postCdr(url, cdrData);
            logger.info("{} postCdr: {}， request url {} , response: {}", cdr.getUuid(),  cdrData, url, response);
            if ("success".equalsIgnoreCase(response)) {
                logger.info("{} post cdr succeed.", cdr.getUuid());
            } else {
                logger.error("{} post cdr failed: cdr data={}", cdr.getUuid(), cdrData);
            }
        }catch (Throwable e){
            logger.error("postCdr error: {} {}", e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
           new Thread(new Runnable() {
               @SneakyThrows
               @Override
               public void run() {
                   try {
                       logger.info("CdrPush thread is now running...");
                       while (true) {
                           semaphore.acquire();
                           CdrDetail cdrDetail = cdrQueue.poll();
                           if (null != cdrDetail) {
                               postCdr(cdrDetail);
                           }
                           Thread.sleep(10);
                       }
                   }catch (Throwable e){
                       logger.error("postCdr main thread error: {} {}", e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
                   }
               }
           }).start();
    }
}

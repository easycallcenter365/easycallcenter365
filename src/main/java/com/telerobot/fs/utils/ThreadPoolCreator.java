package com.telerobot.fs.utils;

import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  线程池工具类;
 * @author easycallcenter365@gmail.com
 */
public class ThreadPoolCreator {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ThreadPoolCreator.class);

    /**
     * 压力测试工作线程池;
     * 自定义ThreadFactory设置线程的名称; (便于在调试的时候识别它们)
     */
      static    class CustomThreadFactory implements ThreadFactory {
         String name="";
         public  CustomThreadFactory(String name){
             this.name = name;
         }
        private AtomicInteger count = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            String threadName = name + "_" + count.addAndGet(1);
            LOGGER.info(" create " + threadName);
            t.setName(threadName);
            return t;
        }
    }

    /**
     * \ 创建一个线程池;
     * @param poolSize  线程池最大工作线程数量
     * @param name  线程名称
     * @param keepAliveTime 线程保活时间
     * @param taskQueueSize  任务队列的最大容量
     * @return
     */
     public static   ThreadPoolExecutor create(int poolSize, String name, long keepAliveTime, int taskQueueSize)   {
        ThreadPoolExecutor workerThreadPool = new ThreadPoolExecutor(poolSize,  poolSize + 1,  keepAliveTime,
                TimeUnit.HOURS, new ArrayBlockingQueue<Runnable>(taskQueueSize),
                new CustomThreadFactory(name));
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                LOGGER.error(name + " thread pool is full, cant not add task: " + r.toString());
            }
        };
        workerThreadPool.setRejectedExecutionHandler(handler);
        LOGGER.info("Successfully create thread pool:" + name);
        return workerThreadPool;
    }
}
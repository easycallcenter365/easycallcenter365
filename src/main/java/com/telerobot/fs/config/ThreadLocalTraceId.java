package com.telerobot.fs.config;

public class ThreadLocalTraceId {
    private  ThreadLocal<String> traceIdList;
    
    private ThreadLocalTraceId(){
        traceIdList = new ThreadLocal<String>();
    }

    /**
     * 获取traceid
     * @return
     */
    public  String getTraceId() {
        return traceIdList.get();
    }

    /**
     *  设置traceid
     * @param message
     */
    public  void setTraceId(String message) {
        traceIdList.set(message);
    }
    
    private static ThreadLocalTraceId threadLocalMessage = new ThreadLocalTraceId();
    
    public static ThreadLocalTraceId getInstance(){
        return threadLocalMessage;
    }
}
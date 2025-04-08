package com.telerobot.fs.wshandle.impl;

import java.util.Map;

/**
 *  当获取到BgApi执行的结果的事件
 */
public interface IOnRecvBgApiResult {

    /**
     * on backgroundJobResultReceived
     * @param traceId traceId
     */
    void onRecv(String apiResponse, String traceId);

}

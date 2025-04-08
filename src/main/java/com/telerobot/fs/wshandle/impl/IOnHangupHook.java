package com.telerobot.fs.wshandle.impl;

import java.util.Map;

public interface IOnHangupHook {

    /**
     * 通道挂机回调;
     * @param traceId traceId
     */
    void onHangup(Map<String,String> eventHeaders, String traceId);

}

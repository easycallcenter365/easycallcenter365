package com.telerobot.fs.wshandle.impl;

import java.util.Map;

public interface IChannelParkHook {

    /**
     * 通道park回调;
     * @param traceId traceId
     */
    void onPark(Map<String,String> eventHeaders, String traceId);

}

package com.telerobot.fs.wshandle.impl;

import java.util.Map;

public interface IOnRecvMediaHook {

    /**
     * recv media event
     * @param traceId traceId
     */
    void onRecvMedia(Map<String,String> eventHeaders, String traceId);

}

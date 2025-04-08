package com.telerobot.fs.wshandle.impl;

import java.util.Map;

public interface IOnAnsweredHook {

    /**
     * 外线通话应答回调;
     * @param traceId traceId
     */
    void onAnswered(Map<String,String> eventHeaders, String traceId);

}

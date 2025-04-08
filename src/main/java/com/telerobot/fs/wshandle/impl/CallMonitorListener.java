package com.telerobot.fs.wshandle.impl;

import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.RespStatus;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CallMonitorListener implements IEslEventListener {

    private CallMonitor callMonitorObject = null;
    private String uuidInner;
    private String traceId = "";
    private String backgroundJobUuid = "";
    private static final Logger logger = LoggerFactory.getLogger(CallListener.class);
    private volatile boolean isHangup = false;

    public boolean getHangup() {
        return isHangup;
    }

    public CallMonitorListener(CallMonitor callMonitor, String uuidInner) {
        this.callMonitorObject = callMonitor;
        this.uuidInner = uuidInner;
    }

    private String getTraceId() {
        if (StringUtils.isNullOrEmpty(traceId)) {
            traceId = callMonitorObject.getTraceId() + " " + uuidInner + ":";
        }
        return traceId;
    }

    public void setBackgroundJobUuid(String backgroundJobUuid) {
        this.backgroundJobUuid = backgroundJobUuid;

    }

    void onDispose() {
        unRegisterListener();
    }

    private void unRegisterListener() {
        EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().removeListener(this.uuidInner);
    }

    /**
     * 结束监听
     */
    public synchronized void endMonitoring() {
        if (!isHangup) {
            EslConnectionUtil.sendSyncApiCommand(
                    "uuid_kill",
                    uuidInner,
                    EslConnectionUtil.getDefaultEslConnectionPool()
            );
        } else {
            callMonitorObject.sendReplyToAgent(new MessageResponse(RespStatus.REQUEST_PARAM_ERROR, "通话已结束，不可监听"));
        }
    }

    @Override
    public void eventReceived(String addr, EslEvent event) {
        Map<String, String> headers = event.getEventHeaders();
        String uniqueId = headers.get("Unique-ID");
        String eventName = headers.get("Event-Name");
        if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {
            if (uniqueId.equalsIgnoreCase(uuidInner)) {
                logger.info("{} 内线已接通，开始监听。", getTraceId());
                callMonitorObject.sendReplyToAgent(new MessageResponse(RespStatus.CALLMONITOR_START, "通话开始监听！"));
            }
        }else if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
            if (uniqueId.equalsIgnoreCase(uuidInner)) {
                isHangup = true;
                logger.info("{} 内线已挂断，监听结束.", getTraceId());
                callMonitorObject.sendReplyToAgent(new MessageResponse(RespStatus.CALLMONITOR_END, "通话监听结束！"));
                onDispose();
            }
        }
    }

    @Override
    public void backgroundJobResultReceived(String addr, EslEvent event) {
        EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().removeListener(this.backgroundJobUuid);
        logger.info("recv BACKGROUND_JOB : {}" , event.toString());
        if (!event.toString().contains("OK")) {
            ProcessDialedFailCase(event.toString());
            this.unRegisterListener();
        }
    }

    /***
     * 处理各种呼叫失败的情况
     ****/
    private void ProcessDialedFailCase(String callResponseStr) {
        MessageResponse response = CommonUtils.sendExtensionErrorInfo(
                callResponseStr,
                callMonitorObject.getSessionInfo().getExtNum()
        );
        callMonitorObject.sendReplyToAgent(
                response
        );
    }
}
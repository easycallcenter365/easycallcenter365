package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.MsgHandlerBase;
import com.telerobot.fs.wshandle.MsgStruct;
import com.telerobot.fs.wshandle.Utils;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;

/**
 * 通话监听：
 *   1.开始监听
 *   2.结束监听
 */
public class CallMonitor extends MsgHandlerBase {

    CallMonitorListener listener = null;

    @Override
    public void processTask(MsgStruct data) {
        //然后，接下来解析参数
        CallArgs callArgs = null;
        try {
            callArgs = JSON.parseObject(data.getBody(), CallArgs.class);
        } catch (Throwable e) {
            Utils.processArgsError("invalid json format:" + e.toString(), this);
            return;
        }
        if (callArgs == null) {
            return;
        }
        String cmd = callArgs.getCmd();
        if (StringUtils.isNullOrEmpty(cmd)) {
            Utils.processArgsError("cmd参数错误", this);
            return;
        }

        // callSpyId参数处理
        String callSpyId = "";
        if("startMonitoring".equals(cmd)) {
            callSpyId = callArgs.getArgs().getString("callSpyId");
            if (StringUtils.isNullOrEmpty(callSpyId)) {
                Utils.processArgsError("callSpyId参数错误", this);
                return;
            }
        }

        /**
         * 然后做一个switch处理 —— 两个动作：开始监听和结束监听
         */
        switch (cmd){
            case "startMonitoring":
                startMonitoring(callSpyId);
            break;
            case "endMonitoring":
                endMonitoring();
            break;
        }
    }

    /**
     * 开始监听
     */
    private void startMonitoring(String callSpyId){
        if(null != listener){
            if(!listener.getHangup()){
                listener.endMonitoring();

                // 等待通话结束
                int counter = 0;
                while (!listener.getHangup() && counter < 200){
                    ThreadUtil.sleep(10);
                    if(counter == 0){
                        logger.info("{} last callMonitor is not hangup, wait for call hangup...", getTraceId());
                    }
                    counter ++;
                }
                logger.info("{} call is hangup, ready to start callMonitor...", getTraceId());
            }
        }
        EslConnectionPool connectionPool = EslConnectionUtil.getDefaultEslConnectionPool();
        if (connectionPool != null) {
            String uuid = UuidGenerator.GetOneUuid();
            listener = new CallMonitorListener(this, uuid);
            connectionPool.getDefaultEslConn().addListener(uuid, listener);

            String extNum = this.getSessionInfo().getExtNum();
            if(extNum.contains("-")){
                extNum = extNum.split("\\-")[0];
            }

            //呼叫字符串
            String originationStr = String.format("{absolute_codec_string=pcma,origination_uuid=%s}user/%s &eavesdrop(%s)",
                    uuid,
                    extNum,
                    callSpyId
            );
            String jobId  = EslConnectionUtil.sendAsyncApiCommand("originate", originationStr, connectionPool);
            logger.info("{} fs bgapi originate 响应: {}", getTraceId(), jobId);
            if(!StringUtils.isNullOrEmpty(jobId)){
                if(this.listener != null){
                    connectionPool.getDefaultEslConn().addListener(jobId.trim(), listener);
                    this.listener.setBackgroundJobUuid(jobId.trim());
                }else{
                    logger.error("{} 无法获取fs的backGroundJobUuid", getTraceId());
                }
            }
        } else {
            Utils.processServerInternalError("Can not connect to freeSwitch.",this, true);
        }
    }

    /**
     * 结束监听
     */
    private void endMonitoring(){
        if (this.getIsDisposed()) {
            return;
        }
        if (this.listener != null) {
            this.listener.endMonitoring();
            this.listener = null;
        }else{
            Utils.processArgsError("通话不存在.", this);
        }
    }
}

package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.wshandle.*;

/**
 *  用户控制； 置忙；置闲；下线；
 */
public class AgentCc extends MsgHandlerBase {
    private AgentCc thisRef = this;
    private SysService sysService = AppContextProvider.getBean(SysService.class);

    @Override
    public void processTask(MsgStruct data) {
        CallArgs callArgs = null;
        try {
            callArgs = JSON.parseObject(data.getBody(), CallArgs.class);
        } catch (Throwable e) {
            Utils.processArgsError("invalid json format:" + e.toString(), thisRef);
            return;
        }
        if (callArgs == null) {
            return;
        }
        String cmd = callArgs.getCmd();
        if (cmd == null || cmd.length() == 0) {
            Utils.processArgsError("cmd param error", thisRef);
            return;
        }
        switch (cmd) {
            case "setStatus":
                int status = callArgs.getArgs().getIntValue("status");
                setStatus(status);
                break;
            case "disconnect":
                MessageHandlerEngineList.getInstance().delete(getSessionInfo().getSessionId(), true);
                break;
            default:
                Utils.processArgsError(String.format("method not supported :%s", cmd), thisRef);
                break;
        }
    }

    /**
     * set agent status.
     * @param status
     */
    protected void setStatus(int status){
        int affectRow = sysService.setAgentStatus(getSessionInfo().getOpNum(), status);
        if(status == AgentStatus.free.getIndex()) {
            this.getSessionInfo().unLock();
        }
        if (affectRow == 1) {
            AgentStatus agentStatus = AgentStatus.getItemByValue(status);
            String description = "cant not get description";
            if (null != agentStatus) {
                description = agentStatus.toString();
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", status);
            sendReplyToAgent(new MessageResponse(RespStatus.STATUS_CHANGED, "agent status: " + description, jsonObject));
        } else {
            sendReplyToAgent(new MessageResponse(RespStatus.SERVER_ERROR, "update agent status error."));
        }
    }

}

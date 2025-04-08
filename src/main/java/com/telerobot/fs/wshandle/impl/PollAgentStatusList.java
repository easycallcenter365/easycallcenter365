package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.service.PollAgentList;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.MsgHandlerBase;
import com.telerobot.fs.wshandle.MsgStruct;
import com.telerobot.fs.wshandle.RespStatus;

public class PollAgentStatusList  extends MsgHandlerBase {
    @Override
    public void processTask(MsgStruct data) {
        JSONObject jsonObject = JSON.parseObject(data.getBody());
        String cmd = jsonObject.getString("cmd");
        switch (cmd){
            case "subscribe":
                PollAgentList.addSubscriber(this);
                sendReplyToAgent(new MessageResponse(
                        RespStatus.AGENT_STATUS_DATA_CHANGED,
                        "data changed.",
                        PollAgentList.getAgentList()
                ));
                break;
            case "unSubscribe":
                PollAgentList.removeSubscriber(this);
                break;
        }

    }

    @Override
    public synchronized void dispose() {
        if(super.getIsDisposed()){
            return;
        }
        PollAgentList.removeSubscriber(this);
        super.dispose();
    }
}

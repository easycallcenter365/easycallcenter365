package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.bo.ChannelFlag;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.*;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;

import java.util.Map;

public class CallHold extends MsgHandlerBase {
   private volatile   SwitchChannel customerChannel = null;

    @Override
    public void processTask(MsgStruct data) {
        MessageResponse msg = new MessageResponse();
        CallArgs callArgs = null;
        try {
            callArgs = JSON.parseObject(data.getBody(), CallArgs.class);
        } catch (Throwable e) {
            msg.setStatus(400);
            msg.setMsg("invalid json format.");
            sendReplyToAgent(msg);
            return;
        }
        if (callArgs == null) {
            return;
        }
        logger.info("{} recv msg: CallApi: {}", getTraceId(), data.getBody());
        String cmd = callArgs.getCmd();
        if (cmd == null || cmd.length() == 0) {
            Utils.processArgsError("cmd param error", this);
            return;
        }
        switch (cmd) {
            case "hold":
                holdCall();
                break;
            case "unhold":
                unHoldCall();
                break;
            default:
                Utils.processArgsError("unSupported command.", this);
        }
    }

    /**
     *  hold call session.
     *
     */
    private void holdCall() {
        if(null != customerChannel){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "The previous call was on hold."
            ));
            return;
        }
        MessageHandlerEngine engine = this.msgHandlerEngine;
        CallApi callApi = ((CallApi) engine.getMessageHandleByName("call"));
        if (null != callApi) {

            if(callApi.listener == null){
                sendReplyToAgent(new MessageResponse(
                        RespStatus.REQUEST_PARAM_ERROR,
                        "No call session found."
                ));
                return;
            }

            customerChannel = callApi.listener.getCustomerChannel();
            if(customerChannel.getHangupTime() > 0L || customerChannel.getAnsweredTime() == 0L ){
                sendReplyToAgent(new MessageResponse(
                        RespStatus.REQUEST_PARAM_ERROR,
                        "Call session is hangup or not ready."
                ));
                return;
            }

            // HOLD Customer CALL
            EslConnectionUtil.sendExecuteCommand(
                    "set",
                    "park_after_bridge=true",
                    customerChannel.getUuid()
            );
            customerChannel.setFlag(ChannelFlag.HOLD_CALL);
            logger.info("{} set customerChannel {} HOLD_CALL and park_after_bridge=true",
                    getTraceId(), customerChannel.getUuid());
            ThreadUtil.sleep(10);

            // hangup extension
            callApi.listener.endCall("call_hold.");
            ThreadUtil.sleep(10);

            EslConnectionUtil.sendExecuteCommand("endless_playback",
                    "$${sounds_dir}/ivr/hold.wav",
                    customerChannel.getUuid()
            );

            customerChannel.setHangupHook(new IOnHangupHook() {
                @Override
                public void onHangup(Map<String, String> eventHeaders, String traceId) {
                    String tips = "customer call on hold is hangup.";
                    engine.sendReplyToAgent(new MessageResponse(
                            RespStatus.CUSTOMER_ON_HOLD_HANGUP, tips, customerChannel)
                    );
                    logger.info(tips);
                    clearHoldCallFlag();
                    customerChannel = null;
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendWsMsg();
                }
            }).start();
        } else {
            engine.sendReplyToAgent(new MessageResponse(
                    RespStatus.SERVER_ERROR, "server internal error!")
            );
        }
    }

    private void sendWsMsg(){
        while(null != customerChannel){
            //send ws msg.
            sendReplyToAgent(new MessageResponse(
                    RespStatus.CUSTOMER_CHANNEL_HOLD, "customer call session hold.", customerChannel)
            );
            ThreadUtil.sleep(2000);
        }
    }

    private void unHoldCall(){
        if(null == customerChannel){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_PARAM_ERROR,
                    "No call is on hold."
            ));
            return;
        }

        CallApi callApi = ((CallApi) this.msgHandlerEngine.getMessageHandleByName("call"));
        if (null == callApi) {
            sendReplyToAgent(new MessageResponse(
                    RespStatus.SERVER_ERROR,
                    "Cant not get CallApi."
            ));
            return;
        }
        String bleg = UuidGenerator.GetOneUuid();
        SwitchChannel agentChannel = new SwitchChannel(
                bleg,
                customerChannel.getUuid(),
                customerChannel.getCallType(),
                CallDirection.INBOUND
        );;
        agentChannel.setAnsweredTime(0L);
        agentChannel.setHangupTime(0L);
        agentChannel.setBridgeCallAfterPark(true);
        agentChannel.setSendChannelStatusToWsClient(true);
        agentChannel.setAnsweredHook(new IOnAnsweredHook() {
            @Override
            public void onAnswered(Map<String, String> eventHeaders, String traceId) {
                logger.info("IOnAnsweredHook executed. uuid={}", bleg);
                sendReplyToAgent(new MessageResponse(
                        RespStatus.CUSTOMER_CHANNEL_UNHOLD, "customer call session unhold.", customerChannel)
                );
                clearHoldCallFlag();
                customerChannel = null;
            }
        });
        customerChannel.setSendChannelStatusToWsClient(true);
        customerChannel.setHangupHook(new IOnHangupHook() {
            @Override
            public void onHangup(Map<String, String> eventHeaders, String traceId) {
                clearHoldCallFlag();
                customerChannel = null;
            }
        });
        setHoldCallFlag();
        customerChannel.setUuidBLeg(bleg);
        callApi.connectExtension(agentChannel, customerChannel);
    }

    private void setHoldCallFlag(){
        if( customerChannel != null){
            customerChannel.setFlag(ChannelFlag.HOLD_CALL);
        }
    }

    private void clearHoldCallFlag(){
        if( customerChannel != null){
            customerChannel.clearFlag(ChannelFlag.HOLD_CALL);
        }
    }

}

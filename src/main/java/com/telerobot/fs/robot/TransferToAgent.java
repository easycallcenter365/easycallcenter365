package com.telerobot.fs.robot;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.acd.AcdSqlQueue;
import com.telerobot.fs.acd.CallHandler;
import com.telerobot.fs.acd.InboundGroupHandler;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.po.CdrDetail;
import com.telerobot.fs.global.CdrPush;
import com.telerobot.fs.utils.CommonUtils;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.management.resources.agent;

import java.util.Map;

/**
 *  TransferToAgent
 */
public class TransferToAgent {

    private static final Logger logger = LoggerFactory.getLogger(TransferToAgent.class);

    /**
     *  transfer to acd queue
     */
    private static final String TRANSFER_TO_ACD = "acd";

    /**
     *  transfer to external gateway
     */
    private static final String TRANSFER_TO_GATEWAY = "gateway";

    /**
     *  transfer a call session to acd queue or an external gateway
     * @param callDetail
     */
    public  static  void transfer(InboundDetail callDetail){
        String configType = SystemConfig.getValue("transfer-to-agent-type");
        if(configType.equalsIgnoreCase(TRANSFER_TO_ACD)) {
            CallHandler callHandler = new CallHandler(callDetail);
            if (InboundGroupHandler.addCallToQueue(callHandler, callDetail.getGroupId())) {
                logger.info("{} Successfully add call to acd queue", callDetail.getUuid());
            }
        }else  if(configType.equalsIgnoreCase(TRANSFER_TO_GATEWAY)) {
            transferToAgentUsingGateway(callDetail);
        }
    }

    private static void transferToAgentUsingGateway(InboundDetail inboundDetail) {
        String destPhone = SystemConfig.getValue("transfer-to-agent-gateway-number");
        String queryString = SystemConfig.getValue("transfer-to-agent-gateway-info");
        // gatewayAddr=192.168.67.201:5090&caller=64901409&profile=external&calleePrefix=
        Map<String, String> gatewayInfo = null;
        try {
            gatewayInfo = CommonUtils.parseUrlQueryString(queryString);
        }catch (Throwable throwable){
            logger.error("{} parse  transfer-to-agent-gateway-info  params error! ", inboundDetail.getUuid());
        }
        if(gatewayInfo == null){
            return;
        }

        String outboundUuid = UuidGenerator.GetOneUuid();
        String bridgeString = String.format(
                "{origination_uuid=%s,origination_caller_id_number=%s,effective_caller_id_number=%s}sofia/%s/%s%s@%s ",
                outboundUuid,
                gatewayInfo.get("caller"),
                gatewayInfo.get("caller"),
                gatewayInfo.get("profile"),
                gatewayInfo.get("calleePrefix"),
                destPhone,
                gatewayInfo.get("gatewayAddr")
        );
        //设置bridge后挂机;
        EslConnectionUtil.sendExecuteCommand(
                "set",
                "hangup_after_bridge=true",
                inboundDetail.getUuid(),
                EslConnectionUtil.getDefaultEslConnectionPool()
        );
        inboundDetail.setCallee(destPhone);
        inboundDetail.setExtnum(destPhone);
        inboundDetail.setOpnum(destPhone);

        IEslEventListener listener = new IEslEventListener() {
            private volatile boolean hangup = false;
            @Override
            public synchronized void eventReceived(String addr, EslEvent event) {
                Map<String, String> headers = event.getEventHeaders();
                String uniqueId = headers.get("Unique-ID");
                String eventName = headers.get("Event-Name");
                if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {
                    inboundDetail.setAnsweredTime(System.currentTimeMillis());
                } else if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
                    if(!hangup){
                        hangup = true;
                        // 推送话单;
                        if(inboundDetail.getAnsweredTime() > 0L) {
                            inboundDetail.setAnsweredTimeLen(System.currentTimeMillis() - inboundDetail.getAnsweredTime());
                        }
                        inboundDetail.setHangupTime(System.currentTimeMillis());
                        AcdSqlQueue.addToSqlQueue(inboundDetail);
                        CdrDetail cdrDetail = new CdrDetail();
                        cdrDetail.setUuid(inboundDetail.getUuid());
                        cdrDetail.setCdrType("inbound");
                        cdrDetail.setCdrBody(JSON.toJSONString(inboundDetail));
                        CdrPush.addCdrToQueue(cdrDetail);
                        logger.info("{} 话单已推送", inboundDetail.getUuid());
                    }
                    String uuidKill = "";
                    if(uniqueId.equals(outboundUuid)){
                        uuidKill = inboundDetail.getUuid();
                    }else{
                        uuidKill = outboundUuid;
                    }
                    EslConnectionUtil.sendAsyncApiCommand("uuid_kill", uuidKill);
                }
            }
            @Override
            public void backgroundJobResultReceived(String addr, EslEvent event) {

            }
        };

        EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().addListener(outboundUuid, listener);
        EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().addListener(inboundDetail.getUuid(), listener);
        EslConnectionUtil.sendExecuteCommand(
                "bridge",
                bridgeString,
                inboundDetail.getUuid()
        );
    }

}

package com.telerobot.fs.wshandle.impl;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dao.ExtPowerConfig;
import com.telerobot.fs.entity.dto.CallMonitorInfo;
import com.telerobot.fs.service.AsrResultListener;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.MsgHandlerBase;
import com.telerobot.fs.wshandle.MsgStruct;
import com.telerobot.fs.wshandle.RespStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 *    呼入电话排队监控;
 *   通过本接口可以一次性获取(有权限的)通话信息列表;
 *
 */
public class InboundMonitorDataPull extends MsgHandlerBase {

    private SysService sysService = AppContextProvider.getBean(SysService.class);

    private static ArrayBlockingQueue<CallMonitorInfo> callMonitorInfoList =
            new ArrayBlockingQueue<>(1000);

    private static ConcurrentHashMap<String,ArrayBlockingQueue<InboundMonitorDataPull>>
            wsClientsMap =  new ConcurrentHashMap<>(20);

    public static void add(CallMonitorInfo callMonitorInfo){
        if(null == callMonitorInfo || callMonitorInfo.getGroupId().equals("0")) {
            return;
        }
        callMonitorInfo.setHangupTimeStamp(0L);
        logger.info("add CallMonitorInfo. uuid={}", callMonitorInfo.getUuid() );
        callMonitorInfoList.add(callMonitorInfo);
        notifyAllSubscribers(callMonitorInfo);
    }

    public static void remove(CallMonitorInfo callMonitorInfo){
        callMonitorInfo.setHangupTimeStamp(System.currentTimeMillis());
        logger.info("remove CallMonitorInfo. uuid={}", callMonitorInfo.getUuid() );
        callMonitorInfoList.remove(callMonitorInfo);
        notifyAllSubscribers(callMonitorInfo);
    }

    private static synchronized void addSubscriber(String groupId, InboundMonitorDataPull obj){
       ArrayBlockingQueue<InboundMonitorDataPull> wsClients = wsClientsMap.get(groupId);
       if(null == wsClients){
           wsClients = new ArrayBlockingQueue<InboundMonitorDataPull>(10);
           wsClientsMap.put(groupId, wsClients);
       }
        wsClients.add(obj);
    }

    private static synchronized void removeSubscriber(String groupId, InboundMonitorDataPull obj){
        ArrayBlockingQueue<InboundMonitorDataPull> wsClients = wsClientsMap.get(groupId);
        if(null != wsClients){
            logger.info("{} removeSubscriber groupId={}", obj.getTraceId(), groupId);
            wsClients.remove(obj);
        }
    }

    private static void notifyWsClients(CallMonitorInfo callMonitorInfo, String groupId){
        ArrayBlockingQueue<InboundMonitorDataPull> wsClients = wsClientsMap.get(groupId);
        if(wsClients != null){
            List<CallMonitorInfo> callList = new ArrayList<CallMonitorInfo>();
            callList.add(callMonitorInfo);
            for (InboundMonitorDataPull wsClient : wsClients) {
                wsClient.sendReplyToAgent(
                        new MessageResponse(
                                RespStatus.CALL_SESSION_STATUS_DATA_CHANGED,
                                "data changed.",
                                callList
                        )
                );
            }
        }
    }

    /**
     *  把状态变更通知到所有订阅者;
     *  当有新通话产生、通话转接时，通知到该通话所属groupId的所有Websocket客户端
     */
    private static void notifyAllSubscribers(CallMonitorInfo callMonitorInfo){
        notifyWsClients(callMonitorInfo, callMonitorInfo.getGroupId());
        notifyWsClients(callMonitorInfo, "0");
    }

    private  List<String> groupIds;

    @Override
    public void processTask(MsgStruct data) {

        String extNum = this.getSessionInfo().getExtNum().split("\\-")[0];
        // 从数据库查询当前分机的权限;
        ExtPowerConfig powerConfig = sysService.getPowerByExtNum(extNum);
        if(powerConfig == null){
            sendReplyToAgent(new MessageResponse(
                    RespStatus.REQUEST_FORBIDDEN, "No power. Forbidden.")
            );
            return;
        }

        groupIds = new ArrayList<>(10);
        String tmpGroupId = powerConfig.getGroupId();
        if(tmpGroupId.contains(",")){
            groupIds.addAll(Arrays.asList(tmpGroupId.split(",")));
        }else{
            groupIds.add(tmpGroupId);
        }

        for (String groupId : groupIds) {
            addSubscriber(groupId, this);
        }

        List<CallMonitorInfo> callList = new ArrayList<CallMonitorInfo>(200);
        // send exists connected call data to wsClient
        for (CallMonitorInfo callInfo : callMonitorInfoList) {
            for (String groupId : groupIds) {
                if(groupId.equalsIgnoreCase(callInfo.getGroupId())){
                    callList.add(callInfo);
                }
                if("0".equals(groupId)){
                    callList.add(callInfo);
                }
            }
        }

        if(callList.size() > 0) {
            this.sendReplyToAgent(
                    new MessageResponse(
                            RespStatus.CALL_SESSION_STATUS_DATA_CHANGED,
                            "data changed.",
                            callList
                    )
            );
        }
    }

    @Override
    public synchronized void dispose() {
        if(super.getIsDisposed()){
            return;
        }
        if(null != groupIds){
            for (String groupId : groupIds) {
                removeSubscriber(groupId, this);
            }
        }
        super.dispose();
    }

}

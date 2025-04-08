package com.telerobot.fs.service;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.RespStatus;
import com.telerobot.fs.wshandle.impl.PollAgentStatusList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *  从数据库中轮询所有坐席的状态
 */
public class PollAgentList {
    protected static final Logger logger = LoggerFactory.getLogger(PollAgentList.class);
    private static  List<AgentEx> agentList = new ArrayList<>(100);
    private static ArrayBlockingQueue<PollAgentStatusList> wsClients =
            new ArrayBlockingQueue<>(100);

    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("start PollAgentList thread.");
                while (true) {
                    pollDataFromDb();
                    ThreadUtil.sleep(50);
                }
            }
        }).start();
    }

    /**
     *  获取所有坐席列表的json字符串
     * @return
     */
    public static String getAgentList(){
        return JSON.toJSONString(agentList);
    }

    public static synchronized void addSubscriber(PollAgentStatusList subscriber){
       if(!wsClients.contains(subscriber)){
           wsClients.add(subscriber);
           logger.info("{} addSubscriber, recv agentStatusList", subscriber.getTraceId());
       }
    }

    public static void removeSubscriber(PollAgentStatusList subscriber) {
        logger.info("{} removeSubscriber, recv agentStatusList", subscriber.getTraceId());
        wsClients.remove(subscriber);
    }

    private static void pollDataFromDb(){
        //仅当有订阅者的时候才轮询数据库;
        if(wsClients.size() > 0) {
            List<AgentEx> oldList  = agentList;

            agentList = AppContextProvider.getBean(SysService.class).getAllUserList();

            List<AgentEx> changeList = new ArrayList<>(agentList.size());
            for (AgentEx newAgent : agentList) {
               if(!oldList.contains(newAgent)){
                   changeList.add(newAgent);
                   continue;
               }

               //根据工号查找
                AgentEx oldAgent = null;
                for (AgentEx old : oldList) {
                    if(old.getOpnum().equalsIgnoreCase(newAgent.getOpnum())){
                        oldAgent = old;
                    }
                }

                //如果状态发生改变则添加到 changeList
                if(oldAgent != null && oldAgent.getAgentStatus() != newAgent.getAgentStatus()){
                    changeList.add(newAgent);
                }
            }

            // 查找已经下线的Agent
            for (AgentEx agent : oldList) {
                if(!agentList.contains(agent)){
                    agent.setLogoutTime(System.currentTimeMillis());
                    changeList.add(agent);
                }
            }
            if(changeList.size() > 0) {
                notifySubscribers(changeList);
            }
        }
    }

    private static void notifySubscribers(List<AgentEx> changeList){
        MessageResponse response = new MessageResponse(
                RespStatus.AGENT_STATUS_DATA_CHANGED,
                "data changed", JSON.toJSONString(changeList)
        );
        for (PollAgentStatusList wsClient : wsClients) {
            wsClient.sendReplyToAgent(response);
        }
    }

}

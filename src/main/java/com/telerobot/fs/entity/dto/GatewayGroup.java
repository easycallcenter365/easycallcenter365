package com.telerobot.fs.entity.dto;

import java.util.List;
import java.util.concurrent.Semaphore;

public class GatewayGroup {

    private String  groupId;

    /**
     * 网关配置信息是否初始化;
     */
    private volatile boolean initialized = false;

    /**
     *  网关组成员列表
     */
    private List<GatewayConfig> gatewayList;

    /**
     * 网关组初始化控制信号量;
     */
    private Semaphore semaphore = new Semaphore(1);

    public boolean getInitialized() {
        return initialized;
    }

    public List<GatewayConfig> getGatewayList() {
        return gatewayList;
    }

    public void setGatewayList(List<GatewayConfig> gatewayList) {
        this.gatewayList = gatewayList;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public   void reInit(){
        initialized = false;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}

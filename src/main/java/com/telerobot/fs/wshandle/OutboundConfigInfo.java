package com.telerobot.fs.wshandle;

/**
 *  外呼配置信息;
 */
public class OutboundConfigInfo {
    /**
     * 外呼的 sofia profile
     */
    private String sipProfile;
    /**
     * 项目编号
     */
    private String projectId;
    /**
     * 被叫前缀
     */
    private String calleePrefix;
    /**
     * 主叫号码
     */
    private String callerNumber;
    /**
     * 网关地址;
     */
    private String gatewayAddress;

    public String getSipProfile() {
        return sipProfile;
    }

    public void setSipProfile(String sipProfile) {
        this.sipProfile = sipProfile;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCalleePrefix() {
        return calleePrefix;
    }

    public void setCalleePrefix(String calleePrefix) {
        this.calleePrefix = calleePrefix;
    }

    public String getCallerNumber() {
        return callerNumber;
    }

    public void setCallerNumber(String callerNumber) {
        this.callerNumber = callerNumber;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }
}

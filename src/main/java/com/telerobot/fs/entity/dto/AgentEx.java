package com.telerobot.fs.entity.dto;

import com.telerobot.fs.wshandle.SessionEntity;

import java.io.Serializable;
import java.util.Objects;

public class AgentEx implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // 主键
    private String extnum; // 分机号
    private String opnum; // 操作员号
    private String groupId; // 组 ID
    private String sessionId; // 会话 ID
    private int agentStatus; // 代理状态
    private long lastHangupTime; // 最后挂机时间
    private long loginTime; // 登录时间
    private long logoutTime; // 登出时间
    private String clientIp; // 客户端 IP
    private long busyLockTime; // 忙碌锁定时间
    private int skillLevel; // 技能等级

    // 无参构造函数
    public AgentEx() {}

    // 全参构造函数
    public AgentEx(String id, String extnum, String opnum, String groupId, String sessionId,
                   int agentStatus, long lastHangupTime, long loginTime, String clientIp,
                   long busyLockTime, int skillLevel) {
        this.id = id;
        this.extnum = extnum;
        this.opnum = opnum;
        this.groupId = groupId;
        this.sessionId = sessionId;
        this.agentStatus = agentStatus;
        this.lastHangupTime = lastHangupTime;
        this.loginTime = loginTime;
        this.clientIp = clientIp;
        this.busyLockTime = busyLockTime;
        this.skillLevel = skillLevel;
    }

    // Getters 和 Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExtnum() {
        return extnum;
    }

    public void setExtnum(String extnum) {
        this.extnum = extnum;
    }

    public String getOpnum() {
        return opnum;
    }

    public void setOpnum(String opnum) {
        this.opnum = opnum;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getAgentStatus() {
        return agentStatus;
    }

    public void setAgentStatus(int agentStatus) {
        this.agentStatus = agentStatus;
    }

    public long getLastHangupTime() {
        return lastHangupTime;
    }

    public void setLastHangupTime(long lastHangupTime) {
        this.lastHangupTime = lastHangupTime;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public long getBusyLockTime() {
        return busyLockTime;
    }

    public void setBusyLockTime(long busyLockTime) {
        this.busyLockTime = busyLockTime;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    public long getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(long logoutTime) {
        this.logoutTime = logoutTime;
    }

    @Override
    public String toString() {
        return "AgentEx{" +
                "id='" + id + '\'' +
                ", extnum='" + extnum + '\'' +
                ", opnum='" + opnum + '\'' +
                ", groupId='" + groupId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", agentStatus=" + agentStatus +
                ", lastHangupTime=" + lastHangupTime +
                ", loginTime=" + loginTime +
                ", clientIp='" + clientIp + '\'' +
                ", busyLockTime=" + busyLockTime +
                ", skillLevel=" + skillLevel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()){ return false;}
        if(this.getOpnum() == null) { return  false; }
        AgentEx that = (AgentEx) o;
        return  this.getOpnum().equalsIgnoreCase(
                that.getOpnum());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.getOpnum()
        );
    }
}

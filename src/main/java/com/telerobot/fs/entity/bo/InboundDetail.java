package com.telerobot.fs.entity.bo;

import com.alibaba.fastjson.annotation.JSONField;
import com.telerobot.fs.utils.StringUtils;

public class InboundDetail {

    private volatile String id;
    private volatile String caller;
    private volatile String callee;
    private volatile long inboundTime = 0L;
    private volatile String groupId = "";
    private volatile long answeredTime = 0L;
    private volatile String extnum ="";
    private volatile String opnum = "";
    private volatile long hangupTime = 0L;
    @JSONField(serialize=false)
    private volatile boolean hangup;
    @JSONField(serialize=false)
    private volatile long transferTime = 0L;
    private volatile long answeredTimeLen;
    private volatile long timeLen = 0;
    private volatile String uuid = "";
    private volatile String wavFile = "";
    private volatile String chatContent = "";
    /**
     *  如果该字段不为零，则是视频通话，否则为音频通话
     */
    private volatile int remoteVideoPort = 0;

    public InboundDetail(String id, String caller, String callee, long inboundTime, String uuid, String wavFile, String groupId, String remoteVideoPort) {
        this.id = id;
        this.caller = caller;
        this.callee = callee;
        this.inboundTime = inboundTime;
        this.uuid = uuid;
        this.wavFile = wavFile;
        this.groupId = groupId;
        if(!StringUtils.isNullOrEmpty(remoteVideoPort)){
            this.remoteVideoPort = Integer.parseInt(remoteVideoPort);
        }
    }

    public InboundDetail() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        this.callee = callee;
    }

    public long getInboundTime() {
        return inboundTime;
    }

    public void setInboundTime(long inboundTime) {
        this.inboundTime = inboundTime;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setRequestSkillLevel(String groupId) {
        this.groupId = groupId;
    }

    public long getAnsweredTime() {
        return answeredTime;
    }

    public void setAnsweredTime(long answeredTime) {
        this.answeredTime = answeredTime;
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

    public long getHangupTime() {
        return hangupTime;
    }

    public void setHangupTime(long hangupTime) {
        this.hangupTime = hangupTime;
    }

    public long getAnsweredTimeLen() {
        return answeredTimeLen;
    }

    public void setAnsweredTimeLen(long answeredTimeLen) {
        this.answeredTimeLen = answeredTimeLen;
    }

    public long getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(long timeLen) {
        this.timeLen = timeLen;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean getHangup() {
        return hangup;
    }

    public void setHangup(boolean hangup) {
        this.hangup = hangup;
    }

    public long getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(long transferTime) {
        this.transferTime = transferTime;
    }

    public String getWavFile() {
        return wavFile;
    }

    public void setWavFile(String wavFile) {
        this.wavFile = wavFile;
    }

    public int getRemoteVideoPort() {
        return remoteVideoPort;
    }

    public void setRemoteVideoPort(int remoteVideoPort) {
        this.remoteVideoPort = remoteVideoPort;
    }

    public String getChatContent() {
        return chatContent;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }
}

package com.telerobot.fs.entity.dao;

import lombok.Data;

@Data
public class CallVoiceNotification {
    private String id;
    private String telephone;
    private long createtime = 0L;
    private int callstatus = 0;
    private long calloutTime = 0L;
    private int callcount = 0;
    private long callEndTime = 0L;
    private int timeLen = 0;
    private int validTimeLen = 0;
    private String uuid = "";
    private long connectedTime = 0L;
    private String hangupCause="";
    private String dialogue;
    private String recordingsFile="";
    private int gatewayId = 0;
    private String gatewayName = "";
    private String voiceFileSavePath = "";
    private String voiceFileUrl = "";
    private String batchId = "";
}
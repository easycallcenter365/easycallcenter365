package com.telerobot.fs.entity.dto;

import lombok.Data;

import java.util.Objects;

@Data
public class CallMonitorInfo {
    /**
     *  call unique id;
     *   客户侧的通话uuid
     */
    private String uuid;

    /**
     *  坐席侧的通话uuid
     */
    private String uuidAgent;

    /**
     * extension number of call-center agent
     */
    private String extNum;

    /**
     *  customer phone number
     */
    private String customerPhone;
    /**
     *  audio or video
     */
    private String callType;
    /**
     *  inbound or outbound
     */
    private String direction;

    /**
     *  call connected time stamp, millisecond
     */
    private long connectedTimeStamp;

    /**
     *  call hangup time stamp, millisecond
     */
    private long hangupTimeStamp;

    /**
     *  inbound/outbound create time
     */
    private long callTime;

    /**
     * business related groupId
     */
    private String groupId;

    public CallMonitorInfo() {
    }

    public CallMonitorInfo(String uuid, String uuidAgent, String extNum, String customerPhone,
                           String callType, String direction, String groupId, Long callTime) {
        this.uuid = uuid;
        this.uuidAgent = uuidAgent;
        this.extNum = extNum;
        this.customerPhone = customerPhone;
        this.callType = callType;
        this.direction = direction;
        this.groupId = groupId;
        this.callTime = callTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()){ return false;}
        if(this.uuid == null) { return  false; }
        CallMonitorInfo that = (CallMonitorInfo) o;
        return this.uuid.equalsIgnoreCase(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}

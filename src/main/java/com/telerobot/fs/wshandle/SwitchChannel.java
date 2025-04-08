package com.telerobot.fs.wshandle;

import com.telerobot.fs.config.CallConfig;
import com.telerobot.fs.entity.bo.ChanneState;
import com.telerobot.fs.entity.bo.ChannelFlag;
import com.telerobot.fs.entity.dto.CallMonitorInfo;
import com.telerobot.fs.wshandle.impl.*;

import java.util.ArrayList;

public class SwitchChannel {

    private volatile String uuid = "";

    private volatile String uuidBLeg = "";

    /**
     *  通话类型: 语音通话还是视频通话;
     */
    private volatile String callType = "";

    /**
     * 分机号或者外线号码;
     */
    private volatile String phoneNumber = "";

    /**
     * 通话应答时间
     */
    private volatile Long answeredTime = 0L;

    private volatile String callDirection = "";

    /**
     * 通话结束时间
     */
    private volatile Long hangupTime = 0L;

    private volatile ChanneState channelState = ChanneState.INIT;

    private volatile String videoLevel = "";

    private volatile IOnAnsweredHook answeredHook;

    private volatile IOnHangupHook hangupHook;

    private volatile IChannelParkHook parkHook;

    private volatile IOnRecvMediaHook recvMediaHook;

    private volatile IOnRecvBgApiResult recvBgApiResultHook;

    /**
     * send call status when channel is answered.
     */
    private volatile boolean sendChannelStatusToWsClient;

    /**
     *  本通道是否启动监听
     */
    private volatile boolean callMonitorEnabled;

    /**
     * 本通道的监听信息
     */
    private volatile CallMonitorInfo callMonitorInfo;

    /**
     * try to bridge call after recv park event.
     */
    private volatile boolean bridgeCallAfterPark;

    /**
     * 录音/录像的路径
     */
    private volatile String recordingFilePath;

    /**
     * 通话的各种(业务)标志数据
     */
    private ArrayList<ChannelFlag> flags = new ArrayList<>(10);

    /**
     * 挂机的sip状态码
     */
    private String hangupSipCode = "";

    public boolean getSendChannelStatusToWsClient() {
        return sendChannelStatusToWsClient;
    }

    public void setSendChannelStatusToWsClient(boolean sendChannelStatusToWsClient) {
        this.sendChannelStatusToWsClient = sendChannelStatusToWsClient;
    }

    public boolean getCallMonitorEnabled() {
        return callMonitorEnabled;
    }

    public void setCallMonitorEnabled(boolean callMonitorEnabled) {
        this.callMonitorEnabled = callMonitorEnabled;
    }

    public CallMonitorInfo getCallMonitorInfo() {
        return callMonitorInfo;
    }

    public void setCallMonitorInfo(CallMonitorInfo callMonitorInfo) {
        this.callMonitorInfo = callMonitorInfo;
    }

    public boolean getBridgeCallAfterPark() {
        return bridgeCallAfterPark;
    }

    public void setBridgeCallAfterPark(boolean bridgeCallAfterPark) {
        this.bridgeCallAfterPark = bridgeCallAfterPark;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuidBLeg() {
        return uuidBLeg;
    }

    public void setUuidBLeg(String uuidBLeg) {
        this.uuidBLeg = uuidBLeg;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getAnsweredTime() {
        return answeredTime;
    }

    public void setAnsweredTime(Long answeredTime) {
        this.answeredTime = answeredTime;
    }

    public Long getHangupTime() {
        return hangupTime;
    }

    public void setHangupTime(Long hangupTime) {
        this.hangupTime = hangupTime;
    }

    public String getHangupSipCode() {
        return hangupSipCode;
    }

    public void setHangupSipCode(String hangupSipCode) {
        this.hangupSipCode = hangupSipCode;
    }

    public ChanneState getChannelState() {
        return channelState;
    }

    public void setChannelState(ChanneState channeState) {
        this.channelState = channeState;
    }

    public String getVideoLevel() {
        return videoLevel;
    }

    public void setVideoLevel(String videoLevel) {
        this.videoLevel = videoLevel;
    }

    public IOnHangupHook getHangupHook() {
        return hangupHook;
    }

    public void setHangupHook(IOnHangupHook hangupHook) {
        this.hangupHook = hangupHook;
    }

    public IOnRecvMediaHook getRecvMediaHook() {
        return recvMediaHook;
    }

    public void setRecvMediaHook(IOnRecvMediaHook recvMediaHook) {
        this.recvMediaHook = recvMediaHook;
    }

    public IChannelParkHook getParkHook() {
        return parkHook;
    }

    public IOnRecvBgApiResult getRecvBgApiResultHook() {
        return recvBgApiResultHook;
    }

    public void setRecvBgApiResultHook(IOnRecvBgApiResult recvBgApiResultHook) {
        this.recvBgApiResultHook = recvBgApiResultHook;
    }

    public IOnAnsweredHook getAnsweredHook() {
        return answeredHook;
    }

    public void setAnsweredHook(IOnAnsweredHook answeredHook) {
        this.answeredHook = answeredHook;
    }

    public void setParkHook(IChannelParkHook parkHook) {
        this.parkHook = parkHook;
    }

    public String getCallDirection() {
        return callDirection;
    }

    public void setCallDirection(String callDirection) {
        this.callDirection = callDirection;
    }

    public String getRecordingFilePath() {
        return recordingFilePath;
    }

    /**
     *  获取完整的录音/录像的路径;
     * @return
     */
    public String getRecordingFilePathFull() {
        return CallConfig.RECORDINGS_PATH + recordingFilePath;
    }

    public void setRecordingFilePath(String recordingFilePath) {
        this.recordingFilePath = recordingFilePath;
    }

    public SwitchChannel(String uuid, String uuidBLeg, String callType, String callDirection) {
        this.setChannelState(ChanneState.INIT);
        this.uuid = uuid;
        this.uuidBLeg = uuidBLeg;
        this.callType = callType;
        this.callDirection = callDirection;
    }

    /**
     *  检测指定的当前对象的flags中是否有指定的flag
     * @param flag
     * @return
     */
    public synchronized boolean testFlag(ChannelFlag flag) {
        for (ChannelFlag channelFlag : flags) {
            if(channelFlag.getIndex() == flag.getIndex()){
                return true;
            }
        }
        return false;
    }

    public synchronized   void setFlag(ChannelFlag flag) {
        boolean found = false;
        for (ChannelFlag channelFlag : flags) {
            if(channelFlag.getIndex() == flag.getIndex()){
                found = true;
            }
        }
        if(!found){
            flags.add(flag);
        }
    }

    public synchronized   void clearFlag(ChannelFlag flag) {
        flags.remove(flag);
    }

}

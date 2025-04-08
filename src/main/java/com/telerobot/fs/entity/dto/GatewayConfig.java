package com.telerobot.fs.entity.dto;

import com.telerobot.fs.utils.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.concurrent.Semaphore;

public class GatewayConfig {

    private long updateTime;

    /**
     * 类的md5信息，用于判断字段值是否有改变：
     */
    private volatile String md5Info;

    /**
     *  外呼环境
     */
    private String callProfile = "external";

    /**
     * 实时可用并发数
     */
    private Semaphore availableConcurrency;

    /**
     *  网关编号;
     */
    private String uuid;

    /**
     * 网关地址 + 外呼环境[可选参数];
     */
    private String gatewayAddr;
    /**
     * 主叫号码
     */
    private String callerNumber;
    /**
     * 被叫前缀
     */
    private String  calleePrefix;
    /**
     * 优先级
     */
    private Integer  priority =  0;
    /**
     * 并发数
     */
    private Integer concurrency = 1;
    /**
     * 是否注册模式
     */
    private boolean  register  = false;
    /**
     * 语音编码，可用选择项 g711、g729
     */
    private String audioCodec   = "g711";

    public GatewayConfig() {
    }

    public GatewayConfig(long updateTime, String callProfile, String uuid, String gatewayAddr, String callerNumber, String calleePrefix, Integer priority, Integer concurrency, boolean register, String audioCodec) {
        this.updateTime = updateTime;
        this.callProfile = callProfile;
        this.uuid = uuid;
        this.gatewayAddr = gatewayAddr;
        this.callerNumber = callerNumber;
        this.calleePrefix = calleePrefix;
        this.priority = priority;
        this.concurrency = concurrency;
        this.register = register;
        this.audioCodec = audioCodec;
    }

    public String getGatewayAddr() {
        return gatewayAddr;
    }

    public void setGatewayAddr(String gatewayAddr) {
        this.gatewayAddr = gatewayAddr;
    }

    public String getCallerNumber() {
        return callerNumber;
    }

    public void setCallerNumber(String callerNumber) {
        this.callerNumber = callerNumber;
    }

    public String getCalleePrefix() {
        return calleePrefix;
    }

    public void setCalleePrefix(String calleePrefix) {
        this.calleePrefix = calleePrefix;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public boolean getRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getUuid() {
        return uuid;
    }


    public Semaphore getAvailableConcurrency() {
        return availableConcurrency;
    }

    public void setAvailableConcurrency(Semaphore availableConcurrency) {
        this.availableConcurrency = availableConcurrency;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCallProfile() {
        return callProfile;
    }

    public void setCallProfile(String callProfile) {
        this.callProfile = callProfile;
    }

    public String getMd5Info() {
        if(StringUtils.isNullOrEmpty(md5Info)){
            synchronized (this){
                if(StringUtils.isNullOrEmpty(md5Info)){
                    String input = String.format("%s %s %s %s %s %s %s %s",
                            uuid,
                            gatewayAddr.contains(";") ? gatewayAddr : (gatewayAddr + ";" + callProfile),
                            callerNumber,
                            calleePrefix,
                            priority,
                            concurrency,
                            register,
                            audioCodec.equalsIgnoreCase("g711") ? "pcma" : audioCodec
                     );
                    md5Info = DigestUtils.md5DigestAsHex(input.getBytes());
                }
            }
        }
        return md5Info;
    }

    public void setMd5Info(String md5Info) {
        this.md5Info = md5Info;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object obj) {
        //先对地址值进行判断
        if (this == obj) {
           return true;
        }
        return  (obj instanceof GatewayConfig)  && this.uuid.equals(
                  ((GatewayConfig)obj).uuid
               );
    }
}

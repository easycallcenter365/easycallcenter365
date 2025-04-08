package com.telerobot.fs.entity.pojo;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class AsrEntity {

    /**
     *  坐席侧的通话uuid
     */
    private String uuidAgent = "";

    /**
     *  客户侧的通话uuid
     */
    private String uuidCustomer = "";

    /**
     * 分机号
     */
    private String  extnum =  "";

    /**
     *  客户号码
     */
    private String customerPhone =  "";

    /**
     * asr启动时间
     */
    private Long   startTime  =  0L;

    /**
     * 挂机时间
     */
    private long hangupTime = 0L;


    /**
     * asr识别结果集合(本次通话所有角色的asr识别结果)
     */
    private ArrayBlockingQueue<AsrResult> asrResults = new ArrayBlockingQueue<>(10000);

}

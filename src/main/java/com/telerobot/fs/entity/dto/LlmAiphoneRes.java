package com.telerobot.fs.entity.dto;

import lombok.Data;

@Data
public class LlmAiphoneRes {

    /**
     * 生成的回复话术文本
     */
    private String body = "";

    /**
     *  是否挂机:0-不挂机；1-挂机
     */
    private Integer close_phone = 0;

    /**
     *  是否可以被打断：0-不能被打断；1-可以被打断
     */
    private Integer ifcan_interrupt = 0;

    /**
     *  流式接口有/非流式接口没有；1-文本传送完成，0-文本未传送完成
     */
    private Integer finish = 1;

    /**
     * 接口状态码：1-正常；0-失败
     */
    private Integer status_code  = 1;

    /**
     * 接口状态描述
     */
    private String status = "";

    /**
     * tts合成文件路径（预合成）
     */
    private String ttsFilePath;

    /**
     * 流式合成文本
     */
    private String streamTtsText;

    /**
     *  耗时（毫秒）
     */
    private Long costTime = 0L;

    private volatile boolean jsonResponse = false;
}

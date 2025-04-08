package com.telerobot.fs.entity.pojo;

import lombok.Data;

@Data
public class AsrResult {

    /**
     *  1 完整结果； 0 中间结果
     */
    private int vadType = 1;

    /**
     *  语音识别文本
     */
    private String text = "";

    /**
     * 说话结果产生时间
     */
    private Long vadTime = 0L;

    /**
     *  说话这的角色，
     *  1  客户， 2  坐席
     */
    private int role = 1;

    /**
     *  当前事件的唯一编号
     */
    private String vadId = "";

}

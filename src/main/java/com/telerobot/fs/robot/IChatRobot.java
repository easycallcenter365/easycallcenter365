package com.telerobot.fs.robot;

import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;

public interface IChatRobot {

    /**
     *  和 大模型/智能体 对话
     * @param question 问题
     * @return LlmAiphoneRes类型
     */
    LlmAiphoneRes talkWithAiAgent(String question);

    /**
     *  设置通话的uuid
     * @param uuid
     */
    void setUuid(String uuid);


    /**
     *  通话记录信息
     */
    void setCallDetail(InboundDetail callDetail);

    /**
     *  获取对话内容
     * @return
     */
    String getDialogues();

    /**
     *  发送tts请求
     * @param text
     */
    void sendTtsRequest(String text);

    /**
     *  关闭tts通道
     */
    void closeTts();

    /**
     *  标记语音合成的tts通道状态
     */
    void setTtsChannelState(boolean closed);
}

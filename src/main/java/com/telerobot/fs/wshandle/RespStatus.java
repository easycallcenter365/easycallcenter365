package com.telerobot.fs.wshandle;

public class RespStatus {
    /**
     * 客户端提交参数错误;
     */
    public static final int REQUEST_PARAM_ERROR = 400;

    /**
     * 客户端无权请求，权限不足;
     */
    public static final int REQUEST_FORBIDDEN = 403;

    /**
     *  服务器内部错误
     */
    public static final int SERVER_ERROR = 500;

    /**
     *  服务器内部错误，语音编码不匹配
     */
    public static final int SERVER_ERROR_AUDIO_CODEC_NOT_MATCH = 501;


    /**
     * websocketServer连接成功
     */
    public static final int WS_CONNECTED =  200;
    /**
     * 用户下线
     */
    public static final int WS_DISCONNECTED  =  201;

    /**
     *  call_session_status_data_changed
     */
    public static final int CALL_SESSION_STATUS_DATA_CHANGED =  203;

    /**
     *  agent_status_data_changed
     */
    public static final int AGENT_STATUS_DATA_CHANGED =  205;

    /**
     * 主叫接通
     */
    public static final int CALLER_ANSWERED  =  600;
    /**
     * 主叫挂断
     */
    public static final int CALLER_HANGUP  =  601;
    /**
     * 主叫忙; 上一通电话未挂机
     */
    public static final int CALLER_BUSY  =  602;
    /**
     * 主叫未登录
     */
    public static final int CALLER_NOT_LOGIN  =  603;
    /**
     * 主叫应答超时
     */
    public static final int CALLER_RESPOND_TIMEOUT  =  604;
    /**
     * 被叫接通
     */
    public static final int CALLEE_ANSWERED  =  605;
    /**
     * 被叫挂断
     */
    public static final int CALLEE_HANGUP  =  606;
    /**
     * 被叫振铃
     */
    public static final int CALLEE_RINGING =  607;
    /**
     * 座席状态改变
     */
    public static final int STATUS_CHANGED =  608;

    /**
     * 通话监听结束：
     */
    public static final int CALLMONITOR_END =  609;

    /**
     * 通话监听开始：
     */
    public static final int CALLMONITOR_START =  610;

    /**
     * 一个完整的外呼任务结束： [可能尝试了一个或多个网关]
     */
    public static final int OUTBOUND_FINISHED =  611;

    /**
     * 预测外呼，分配的来电;
     */
    public static final int PREDICTIVECALL_INBOUND =  612;

    /**
     * ACD队列分配的新来电
     */
    public static final int NEW_INBOUND_CALL =  613;

    /**
     * 当前业务组实时排队人数
     */
    public static final int ACD_GROUP_QUEUE_NUMBER =  615;

    /**
     * 收到转接的来电请求
     */
    public static final int TRANSFER_CALL_RECV =  616;

    /**
     * 锁定坐席失败
     */
    public static final int LOCK_AGENT_FAIL =  617;

    /**
     * 通话已经转接成功
     */
    public static final int TRANSFER_CALL_SUCCESS=  618;

    /**
     * 产生asr语音识别结果
     */
    public static final int ASR_RESULT_GENERATE =  619;

    /**
     * ASR语音识别流程结束（坐席侧）
     */
    public static final int ASR_PROCESS_END_AGENT =  620;

    /**
     * ASR语音识别流程结束（客户侧）
     */
    public static final int ASR_PROCESS_END_CUSTOMER =  621;

    /**
     * asr语音识别流程已启动
     */
    public static final int ASR_PROCESS_STARTED =  622;


    /**
     * customer call session hold.
     */
    public static final int CUSTOMER_CHANNEL_HOLD =  623;

    /**
     * customer call session unHold.
     */
    public static final int CUSTOMER_CHANNEL_UNHOLD =  624;

    /**
     * customer call session on hold is hangup.
     */
    public static final int CUSTOMER_ON_HOLD_HANGUP =  625;


    public static final int INNER_CONSULTATION_REQUEST = 626;

    /**
     * 多人电话会议，重复的被叫;
     */
    public static final int CONFERENCE_REPEAT_CALLEE =  660;

    /**
     * 多人电话会议，呼叫成员超时;
     */
    public static final int CONFERENCE_CALL_MODERATOR_TIMEOUT =  661;

    /**
     * 多人电话会议，成员接通;
     */
    public static final int CONFERENCE_MEMBER_ANSWERED =  662;


    /**
     * 多人电话会议，成员挂机;
     */
    public static final int CONFERENCE_MEMBER_HANGUP =  663;

    /**
     * 多人电话会议，成员禁言成功;
     */
    public static final int CONFERENCE_MEMBER_MUTED_SUCCESS =  666;


    /**
     * 多人电话会议，成员禁言失败;
     */
    public static final int CONFERENCE_MEMBER_MUTED_FAILED = 665 ;

    /**
     * 多人电话会议，成员解除禁言成功;
     */
    public static final int CONFERENCE_MEMBER_UNMUTED_SUCCESS =  667;


    /**
     * 多人电话会议，成员解除禁言失败;
     */
    public static final int CONFERENCE_MEMBER_UNMUTED_FAILED = 668 ;

    /**
     * 多人电话会议，会议成员不存在，无法执行相关操作：
     */
    public static final int CONFERENCE_MEMBER_NOT_EXISTS = 669 ;

    /**
     * 多人电话会议，主持人重置会议;
     */
    public static final int CONFERENCE_MODERATOR_RESET = 670 ;

    /**
     * 多人电话会议，主持人接通;
     */
    public static final int CONFERENCE_MODERATOR_ANSWERED = 671 ;


    /**
     * 多人电话会议，主持人挂机，会议结束;
     */
    public static final int CONFERENCE_MODERATOR_HANGUP = 672 ;

    /**
     * 多人电话会议，会议布局改变;
     */
    public static final int CONFERENCE_LAYOUT_CHANGED = 673 ;

    /**
     * 多人电话会议，成员视频禁用成功;
     */
    public static final int CONFERENCE_MEMBER_VMUTED_SUCCESS =  674;


    /**
     * 多人电话会议，成员视频禁用失败;
     */
    public static final int CONFERENCE_MEMBER_VMUTED_FAILED = 675 ;

    /**
     * 多人电话会议，成员解除视频禁用成功;
     */
    public static final int CONFERENCE_MEMBER_UnVMUTED_SUCCESS =  676;


    /**
     * 多人电话会议，成员解除视频禁用失败;
     */
    public static final int CONFERENCE_MEMBER_UnVMUTED_FAILED = 677 ;


    /**
     * 多人电话会议，成功把现有通话转接到多人视频会议；
     */
    public static final int CONFERENCE_TRANSFER_SUCCESS_FROM_EXISTED_CALL =  678;
}

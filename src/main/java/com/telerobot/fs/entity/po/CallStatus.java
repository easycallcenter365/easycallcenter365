package com.telerobot.fs.entity.po;

public class CallStatus {

    /**
     * 正在呼叫
     */
    public static final int CALL_ING =  2;

    /**
     * 未接通
     */
    public static final int CALL_NOT_CONNECTED =  3;

    /**
     * 已接通
     */
    public static final int CALL_CONNECTED =  4;

    /**
     *  呼损
     */
    public static final int CALL_LOSS =  5;

    /**
     * 成功转接座席
     */
    public static final int CALL_TRANSFERRED_SUCCESS =  6;

    /**
     * 线路故障
     */
    public static final int LINE_ERROR =  17;

    /**
     *  接通但没有成功转接到坐席; 可能由于分机异常;
     */
    public static  final  int TRANSFERRED_FAILED = 10;
}

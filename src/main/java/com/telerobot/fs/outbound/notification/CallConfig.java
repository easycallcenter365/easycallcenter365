package com.telerobot.fs.outbound.notification;


import com.telerobot.fs.config.SystemConfig;

public class CallConfig {

	/**
	 *  纯AI外呼;
	 */
	public static final int CALL_TYPE_PURE_AI_CALL = 1;
	/**
	 *  人机耦合;
	 */
	public static final int CALL_TYPE_MAN_WITH_AI_CALL = 2;


    /**
     * 每个呼叫call的时间间隔，（毫秒）
     */
    public static  int singleCallDelay = Integer.parseInt(SystemConfig.getValue("voice-notification-single-call-delay", "10"));


    /**
     * 人机耦合ai外呼-系统最大外呼并发量
     */
    public static  int maxCallConcurrency = Integer.parseInt(SystemConfig.getValue("outbound-max-line-number", "1000"));

    /**
     *   呼叫超时（毫秒）
     */
    public static  int callTimeOut =   Integer.parseInt(SystemConfig.getValue("voice-notification-call-time-out", "45000"));

	/**
	 *  当前批次可用线路数目为零，任务暂停
	 */
	public static  int waitDelayWhenNoAvailableLine = Integer.parseInt(SystemConfig.getValue("voice-notification-wait-delay-while-no-available-line", "7000"));


	/** (predict-call)系统总的已用线路数 **/
    private static  int maxLineNumber_Used = 0;

    /**
	 * @return 获取已经使用的外线通道数目
	 */
	public static int getMaxLineNumber_Used() {
		return maxLineNumber_Used;
	}
	private static final Object locker = new Object();
	/**
	 *   外线通道数加1
	 */
	public static  void addMaxLineNumber_Used() {
		synchronized(locker){
			maxLineNumber_Used += 1;
		}
	}
	
	/**
	 *   外线通道数加指定的数目
	 */
	public static  void addMaxLineNumber_Used(int addNum) {
		synchronized(locker){
			maxLineNumber_Used += addNum;
		}
	}
	
	/**
	 *  外线通道数减1
	 */
	public static void releaseLine_Used() {
		synchronized(locker){
			maxLineNumber_Used -= 1;
		}
	}
}

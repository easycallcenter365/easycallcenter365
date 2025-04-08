package com.telerobot.fs.enums;

import java.util.Objects;

public enum RingStatusEnum {
	ASSISTANT(10, "语音助手", "留言,结束请挂机,语音信箱服务,语音助手,秘书,助理"),
	NO_ANSWER(1, "无人接听", "无人接听,暂时无人"),
	BUSY(2, "占线", "用户正忙,正在通话中,电话通话中,电话正在通话"),
	EMPTY(3, "空号", "是空号,号码不存在"),
	OFF(4, "关机", "用户已关机,电话已关机"),
	STOP(5, "停机", "已停机,已暂停服务"),
	NOT_AVAILABLE(6, "无法接通", "暂时无法接通,不在服务区"),
	REMINDER(7, "来电提醒", "来电提醒,来电信息将以短信,短信提醒,短信通知,短信的形式,启动通信助理"),
	LIMIT(8, "呼入限制", "已呼入限制,已互祝限制");

	private Integer code;

	private String name;

	private String keywords;

	RingStatusEnum(Integer code, String name, String keywords) {
		this.code = code;
		this.name = name;
		this.keywords = keywords;
	}

	public Integer getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getKeywords() {
		return keywords;
	}

	public static String getNameByCode(Integer code) {
		for (RingStatusEnum value : RingStatusEnum.values()) {
			if (Objects.equals(value.code, code)) {
				return value.name;
			}
		}
		return null;
	}
}

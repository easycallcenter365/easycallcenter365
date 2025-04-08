package com.telerobot.fs.wshandle;

import java.io.Serializable;

public class MessageResponseEx implements Serializable {

	private static final long serialVersionUID = -2999571571280318844L;
	
	/**
	 * 事件的Id，用于客户端的异步请求
	 */
	private String eventId;
	
	/**
	 * 事件的Name，用于客户端的异步请求
	 */
	private String eventName;
	
	/**
	 * 操作的状态码，以此判断失败的各种原因
	 */
	private int statusCode;
	
	/**
	 * 提示信息
	 */
	private String messageInfo;
	
	/**
	 * 额外的对象信息，便于客户端使用
	 */
	private Object object;
	
	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public String getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(String messageInfo) {
		this.messageInfo = messageInfo;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String generatedId) {
		this.eventId = generatedId;
	}
	
	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	public int getErrorCode() {
		return statusCode;
	}

	public void setErrorCode(int errorCode) {
		this.statusCode = errorCode;
	}
	
}

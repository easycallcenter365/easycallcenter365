package com.telerobot.fs.wshandle;

import com.alibaba.fastjson.JSON;

/**
 * 客户端发送过来的WebSocket消息
 * @author easycallcenter365@gmail.com
 * @date 2018年11月5日 上午9:55:59
 */
public class MsgStruct {
	public MsgStruct() {

	}
	private String action;
	private String body;
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
	
}

package com.telerobot.fs.entity.pojo;

import java.util.Map;

/**
 * 消息体(服务器端发送给客户端的)
 */
public class BaseMessage {
	
	//消息头：key
	 String header;
	
	//消息描述
	 String content;
	
	//请求参数信息集合
	 Object data;
	 
	 
	public BaseMessage(String header, String content) {
		super();
		this.header = header;
		this.content = content;
	}

	public BaseMessage(String header, String content, Object  data) {
		super();
		this.header = header;
		this.content = content;
		this.data = data;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Object getData() {
		return data;
	}

	public void setData(Map<String, String>  mydata) {
		this.data = mydata;
	}
}

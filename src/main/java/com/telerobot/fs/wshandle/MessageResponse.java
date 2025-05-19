package com.telerobot.fs.wshandle;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;

public class MessageResponse implements Serializable {

	private static final long serialVersionUID = -2999571571280318844L;
	
	public MessageResponse(){}
	
	public MessageResponse(int status, String msg, Object object){
		this.status = status;
		this.msg = msg;
		this.object = object;
	}
	
	public MessageResponse(int status, String msg){
		this.status = status;
		this.msg = msg;
		this.object = null;
	}
	
	/**
	 * 操作的返回的代码
	 */
	private int status=200;
	 
	/**
	 * 验证不通过
	 * @return 验证不通过返回true，通过返回false
	 */
	public Boolean checkInvalid(){
		return status != 200;
	}
	
	/**
	 * 提示信息
	 */
	private String msg="";
	
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

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int code) {
		this.status = code;
	}

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
}

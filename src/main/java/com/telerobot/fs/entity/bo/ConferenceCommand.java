package com.telerobot.fs.entity.bo;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConferenceCommand {

	private String method="";
	private ConfMember[] memberList;
	private JSONObject args;

	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}

	public ConfMember[] getMemberList() {
		return memberList;
	}
	public List<ConfMember> getPhoneListEx() {
		List<ConfMember> list = new ArrayList<>(8);
		if(null == memberList) {
			return list;
		}
		for(ConfMember m : memberList){
			if(m.getPhone() != null && m.getPhone().trim().length()>1){
				list.add(m);
			}
		}
		return list;
	}
	public void setMemberList(ConfMember[] memberList) {
		this.memberList = memberList;
	}

	public JSONObject getArgs() {
		return args;
	}
	public void setArgs(JSONObject args) {
		this.args = args;
	}
}

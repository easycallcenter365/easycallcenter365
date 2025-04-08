package com.telerobot.fs.entity.po;

import com.telerobot.fs.entity.pojo.AgentStatus;

/**
 * 坐席信息
 **/
public class AgentEntity {
	private String Id;
	private String extnum;
	private String opnum;
	private String groupId;
	private String sessionId;
	private AgentStatus agentStatus;
	private Long loginTime;
	private String clientIp;
	private int skillLevel;

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	public String getExtnum() {
		return extnum;
	}

	public void setExtnum(String extnum) {
		this.extnum = extnum;
	}

	public String getOpnum() {
		return opnum;
	}

	public void setOpnum(String opnum) {
		this.opnum = opnum;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(Long loginTime) {
		this.loginTime = loginTime;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public int getSkillLevel() {
		return skillLevel;
	}

	public void setSkillLevel(int skillLevel) {
		this.skillLevel = skillLevel;
	}

	public AgentStatus getAgentStatus() {
		return agentStatus;
	}

	public void setAgentStatus(AgentStatus agentStatus) {
		this.agentStatus = agentStatus;
	}
}

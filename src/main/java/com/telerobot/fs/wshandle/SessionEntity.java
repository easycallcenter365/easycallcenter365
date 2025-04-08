package com.telerobot.fs.wshandle;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.AppContextProvider;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class SessionEntity {
	public SessionEntity() {
		init();
	}
	private String sessionId;
	private String extNum;
	private String opNum;
	private String clientIp;
	private int  skillLevel;
	private Long loginTime;
	private String groupId;
	private long lastActiveTime;
	private long lastHangupTime;
	private static boolean enableHeartBeat = true;
	private static final Object LOCKER_HELPER = new Object();
	private Semaphore lock = new Semaphore(1);
	private volatile boolean lockReleased = false;

	/**
	 * 尝试锁定坐席
	 */
	public boolean tryLock(){
		synchronized (this) {
			if(lockReleased) {
				lockReleased = false;
				return lock.tryAcquire();
			}else{
				return false;
			}
		}
	}

	/**
	 * 解除坐席锁定
	 */
	public  void unLock() {
		if(!lockReleased) {
			synchronized (this) {
				if (!lockReleased) {
					lock.release();
					lockReleased = true;
				}
			}
		}
	}

	/**
	 * 会话超时时间(秒) 
	 */
	private static int timeout = 0;
	public SessionEntity(String _para_SessionID, String _para_ExtNum, String _para_ClientIpAddress) {
		this.sessionId = _para_SessionID;
		this.extNum = _para_ExtNum;
		this.clientIp = _para_ClientIpAddress;
		init();
	}

	private void init(){
		this.lastActiveTime = System.currentTimeMillis();
		if(timeout == 0){
			synchronized (LOCKER_HELPER) {
				if(timeout == 0){
					timeout = Integer.parseInt(AppContextProvider.getEnvConfig("ws-server.ws-session-timeout").trim());
					enableHeartBeat = Boolean.parseBoolean(AppContextProvider.getEnvConfig("ws-server.ws-enable-hearbeat").trim());
				}
			}
		}
	}

	public boolean IsValid() {
		 // 没有启用心跳的情况下，直接返回true
		 if (!enableHeartBeat) {
			 return true;
		 }
		 long now = System.currentTimeMillis();
		 int passedTime = (int)((now - lastActiveTime)/1000);
		 if(passedTime > timeout){
			 return false;
		 }
		return true;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getExtNum() {
		return extNum;
	}

	public void setExtNum(String extNum) {
		this.extNum = extNum;
	}

	public String getOpNum() {
		return opNum;
	}

	public void setOpNum(String opNum) {
		this.opNum = opNum;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void setLastActiveTime(long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	public int getSkillLevel() {
		return skillLevel;
	}

	public void setSkillLevel(int skillLevel) {
		this.skillLevel = skillLevel;
	}


	public Long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(Long loginTime) {
		this.loginTime = loginTime;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public long getLastHangupTime() {
		return lastHangupTime;
	}

	public void setLastHangupTime(long lastHangupTime) {
		this.lastHangupTime = lastHangupTime;
	}

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()){ return false;}
		if(this.getOpNum() == null) { return  false; }
		SessionEntity that = (SessionEntity) o;
		return  this.getOpNum().equalsIgnoreCase(
				that.getOpNum());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				this.getOpNum()
		);
	}

}

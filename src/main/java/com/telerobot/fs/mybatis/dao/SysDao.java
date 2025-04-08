package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.dao.BizGroup;
import com.telerobot.fs.entity.dao.ExtPowerConfig;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.entity.po.AgentEntity;
import com.telerobot.fs.entity.po.SysParams;
import com.telerobot.fs.wshandle.SessionEntity;

import java.util.List;




public interface SysDao {

	List<SysParams> getParamsList();

	int setAgentStatus(String opNum, int status);

	int addOnlineUser(AgentEntity user);

	int removeOnlineUser(List<String> sessionId);

	void removeAllOnlineuser();

	/**
	 * 获取所有空闲的在线座席;
	 * @return
	 */
	List<SessionEntity> getFreeUserList(String groupId);

	ExtPowerConfig getPowerByExtNum(String extNum);

	/**
	 * 获取所有在线座席(无论是忙碌/空闲);
	 * @return
	 */
	List<AgentEx> getAllUserList();

	int setAgentStatusWithBusyLock(String opNum, int status, long busyLockTime);

	int resetAgentBusyLockTime(String opNum);

	int saveHangupTime(String extNum);

	List<SessionEntity> getAgentTalkStartTime(String groupId);

	Double getCallConnectedRateByBatchId(int batchId);

	Double getCallDropRateByBatchId(int batchId);

	Double getAvgRingTimeLenByBatchId(int batchId);

	Double getAvgPlayTimeLenByBatchId(int batchId);

	Double getAvgTalkTimeLenByUserId(int batchId, String userId);

	int resetAgentBusyLockTimeEx(String opnum, long expiredTimeMills);

	List<SessionEntity> selectAgentBusyLockTimeout(long expiredTimeMills);

    List<BizGroup> getAllGroupList();

	LlmKb getKbContentByCat(String cat);
}

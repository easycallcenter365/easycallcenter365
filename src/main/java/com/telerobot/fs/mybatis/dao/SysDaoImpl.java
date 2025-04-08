package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.dao.BizGroup;
import com.telerobot.fs.entity.dao.ExtPowerConfig;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.entity.po.AgentEntity;
import com.telerobot.fs.entity.po.SysParams;
import com.telerobot.fs.mybatis.persistence.SysMapper;
import com.telerobot.fs.wshandle.SessionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDaoImpl implements SysDao {

	@Autowired
	private SysMapper mapper;
	
	@Override
	public List<SysParams> getParamsList() {
		return mapper.getParamsList();
	}


	@Override
	public int setAgentStatus(String opNum, int status) {
		return mapper.setAgentStatus(opNum, status);
	}

	@Override
	public int addOnlineUser(AgentEntity user) {
		return mapper.addOnlineUser(user);
	}

	@Override
	public int removeOnlineUser(List<String> sessionId) {
		return mapper.removeOnlineUser(sessionId);
	}

	@Override
	public void removeAllOnlineuser() {
		 mapper.removeAllOnlineuser();
	}


	@Override
	public List<SessionEntity> getFreeUserList(String groupId) {
		return mapper.getFreeUserList(groupId);
	}

	@Override
	public ExtPowerConfig getPowerByExtNum(String extNum) {
		return mapper.getPowerByExtNum(extNum);
	}

	@Override
	public List<AgentEx> getAllUserList() {
		return mapper.getAllUserList();
	}

	@Override
	public int setAgentStatusWithBusyLock(String opNum, int status, long busyLockTime) {
		return mapper.setAgentStatus(opNum, status, busyLockTime);
	}

	@Override
	public int resetAgentBusyLockTime(String opNum) {
		return mapper.resetAgentBusyLockTime(opNum);
	}

	@Override
	public int saveHangupTime(String extNum) {
		return mapper.saveHangupTime(extNum, System.currentTimeMillis());
	}

	@Override
	public List<SessionEntity> getAgentTalkStartTime(String groupId) {
		return mapper.getAgentTalkStartTime(groupId);
	}

	@Override
	public Double getCallConnectedRateByBatchId(int batchId) {
		return mapper.getCallConnectedRateByBatchId(batchId);
	}

	@Override
	public Double getCallDropRateByBatchId(int batchId) {
		return mapper.getCallDropRateByBatchId(batchId);
	}

	@Override
	public Double getAvgRingTimeLenByBatchId(int batchId) {
		return mapper.getAvgRingTimeLenByBatchId(batchId);
	}

	@Override
	public Double getAvgPlayTimeLenByBatchId(int batchId) {
		return mapper.getAvgPlayTimeLenByBatchId(batchId);
	}

	@Override
	public Double getAvgTalkTimeLenByUserId(int batchId, String userId) {
		return mapper.getAvgTalkTimeLenByUserId(batchId, userId);
	}

	@Override
	public int resetAgentBusyLockTimeEx(String opnum, long expiredTimeMills) {
		return mapper.resetAgentBusyLockTimeout(opnum, expiredTimeMills);
	}

	@Override
	public List<SessionEntity> selectAgentBusyLockTimeout(long expiredTimeMills) {
		return mapper.selectAgentBusyLockTimeout(expiredTimeMills);
	}

	@Override
	public List<BizGroup> getAllGroupList() {
		return mapper.getAllGroupList();
	}

	@Override
	public LlmKb getKbContentByCat(String cat) {
		return mapper.getKbContentByCat(cat);
	}

}

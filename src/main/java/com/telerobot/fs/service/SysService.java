package com.telerobot.fs.service;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dao.BizGroup;
import com.telerobot.fs.entity.dao.ExtPowerConfig;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.entity.po.AgentEntity;
import com.telerobot.fs.entity.po.SysParams;
import com.telerobot.fs.mybatis.dao.SysDao;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.wshandle.SessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysService {

	private final Logger log = LoggerFactory.getLogger(SysService.class);

	@Autowired
	private SysDao sysDao;

 	public void refreshParams() {
		List<SysParams> list = sysDao.getParamsList();
		for (SysParams paramObj : list) {
			SystemConfig.paramsContext.put(
					paramObj.getParamCode(),
					paramObj.getParamValue());
		}
	}

	public int updateParam(String paramCode, String paramValue){
	  return sysDao.updateParam(paramCode, paramValue);
	}

	public int setAgentStatus(String opNum,int status) {
		 return  sysDao.setAgentStatus(opNum, status);
	}

	public List<SessionEntity> getFreeUserList(String groupId) {
		return sysDao.getFreeUserList(groupId);
	}

	public ExtPowerConfig getPowerByExtNum(String extNum) {
		return sysDao.getPowerByExtNum(extNum);
	}

	public List<AgentEx> getAllUserList() {
		return sysDao.getAllUserList();
	}

	public List<BizGroup> getAllGroupList() {
		return sysDao.getAllGroupList();
	}



	/**
	 * 重置座席忙碌锁定时间
	 * @param opNum
	 * @return
	 */
	public int resetAgentBusyLockTime(String opNum) {
		try {
			return sysDao.resetAgentBusyLockTime(opNum);
		} catch (Throwable throwable) {
			log.error("resetAgentBusyLockTime 错误: {} {}",
					throwable.toString(),
					CommonUtils.getStackTraceString(throwable.getStackTrace())
			);
		}
		return 0;
	}

	public int saveHangupTime(String extNum) {
		try {
			return sysDao.saveHangupTime(extNum);
		} catch (Throwable throwable) {
			log.error("saveHangupTime 错误: {} {}",
					throwable.toString(),
					CommonUtils.getStackTraceString(throwable.getStackTrace())
			);
		}
		return 0;
	}

	public List<SessionEntity> getAgentTalkStartTime(String groupId) {
		return sysDao.getAgentTalkStartTime(groupId);
	}

	/**
	 *  计算指定批次的实时接通率;
	 * @param batchId
	 * @return
	 */
	public Double getCallConnectedRateByBatchId(int batchId){
		return  sysDao.getCallConnectedRateByBatchId(batchId);
	}

	/**
	 *   实时计算通话转接之前的放弃率；
	 * @param batchId
	 * @return
	 */
	public Double getCallDropRateByBatchId(int batchId) {
		return sysDao.getCallDropRateByBatchId(batchId);
	}

	/**
	 *  平均振铃时长
	 * @param batchId
	 * @return
	 */
	public Double getAvgRingTimeLenByBatchId(int batchId) {
		return sysDao.getAvgRingTimeLenByBatchId(batchId);
	}

	/**
	 *  机器人转人工之前的平均播报时长；
	 * @param batchId
	 * @return
	 */
	public Double getAvgPlayTimeLenByBatchId(int batchId) {
		return sysDao.getAvgPlayTimeLenByBatchId(batchId);
	}

	/**
	 * 实时计算每个座席的， 平均通话时长；
	 * @param batchId
	 * @param userId
	 * @return
	 */
	public Double getAvgTalkTimeLenByUserId(int batchId, String userId) {
		return sysDao.getAvgTalkTimeLenByUserId(batchId, userId);
	}

	public int resetAgentBusyLockTimeEx(String opnum, long expiredTimeMills) {
		try {
			return sysDao.resetAgentBusyLockTimeEx(opnum, expiredTimeMills);
		} catch (Throwable throwable) {
			log.error("resetAgentBusyLockTimeEx 错误: {} {}",
					throwable.toString(),
					CommonUtils.getStackTraceString(throwable.getStackTrace())
			);
		}
		return 0;
	}

	public List<SessionEntity> selectAgentBusyLockTimeout(long expiredTimeMills) {
		try {
			return sysDao.selectAgentBusyLockTimeout(expiredTimeMills);
		} catch (Throwable throwable) {
			log.error("selectAgentBusyLockTimeout 错误: {} {}",
					throwable.toString(),
					CommonUtils.getStackTraceString(throwable.getStackTrace())
			);
		}
		return null;
	}

	public void setAgentStatusWithBusyLock(String opNum, int index) {
		 sysDao.setAgentStatusWithBusyLock(opNum, index, System.currentTimeMillis());
	}

	public LlmKb getKbContentByCat(String cat){
		return sysDao.getKbContentByCat(cat);
	}
}

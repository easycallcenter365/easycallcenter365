package com.telerobot.fs.mybatis.persistence;

import com.telerobot.fs.entity.dao.BizGroup;
import com.telerobot.fs.entity.dao.ExtPowerConfig;
import com.telerobot.fs.entity.dao.LlmKb;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.entity.po.AgentEntity;
import com.telerobot.fs.entity.po.SysParams;
import com.telerobot.fs.wshandle.SessionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface SysMapper {

	/**
	 * 获取所有系统参数
	 * @return
	 */
	 List<SysParams> getParamsList();

	/**
	 *  设置坐席状态;
	 * @param status free OR busy
	 * @return
	 */
	int setAgentStatus(@Param("opnum") String opnum, @Param("status")  int status);

    int addOnlineUser(@Param("user") AgentEntity user);

	int removeOnlineUser(@Param("optList") List<String> optList);

	void removeAllOnlineuser();

	/**
	 * 获取所有在线座席;
	 * @return
	 */
	List<SessionEntity> getFreeUserList(@Param("groupId") String groupId);

	List<AgentEx> getAllUserList();

	/**
	 *  获取通话中的座席；
	 * @param groupId
	 * @return
	 */
	List<SessionEntity> getAgentTalkStartTime(@Param("groupId") String groupId);

	/**
	 *  设置坐席状态;
	 * @param status free OR busy
	 * @return
	 */
	int setAgentStatus(@Param("opnum") String opnum, @Param("status")  int status, @Param("busyLockTime") long busyLockTime);


	int resetAgentBusyLockTime(@Param("opnum") String opnum);

	int saveHangupTime(@Param("extNum") String extNum, @Param("currentTime") long currentTime);

	Double getCallConnectedRateByBatchId(@Param("batchId") int batchId);

	Double getCallDropRateByBatchId(@Param("batchId") int batchId);

	Double getAvgRingTimeLenByBatchId(@Param("batchId") int batchId);

	Double getAvgPlayTimeLenByBatchId(@Param("batchId") int batchId);

	Double getAvgTalkTimeLenByUserId(@Param("batchId") int batchId, @Param("userId")  String userId);

	int resetAgentBusyLockTimeout(@Param("opnum")String opnum, @Param("expiredTimeMills") long expiredTimeMills);

    ExtPowerConfig getPowerByExtNum(@Param("extNum")String extNum);

    List<BizGroup> getAllGroupList();

	List<SessionEntity> selectAgentBusyLockTimeout(@Param("expiredTimeMills") long expiredTimeMills);

    LlmKb getKbContentByCat(@Param("cat")String cat);
}

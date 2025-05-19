package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.dao.CallVoiceNotification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CcCallVoiceNotificationDao {
    int insert(CallVoiceNotification record);


    int insertBatch(@Param("list") List<CallVoiceNotification> records);


    List<CallVoiceNotification> selectByTelephone(String telephone);

    List<CallVoiceNotification> selectRetryCallPhones(int maxRetry, long timeLimit, long validTime);

    int resetHistoryData(long currentTime,long todayTime);

    int updateBatch(@Param("list") List<CallVoiceNotification> records);

    int deleteBatchByIds(List<String> ids);

    List<CallVoiceNotification> selectByBatchId(String batchId);
}

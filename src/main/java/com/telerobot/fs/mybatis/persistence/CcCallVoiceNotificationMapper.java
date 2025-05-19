package com.telerobot.fs.mybatis.persistence;

import com.telerobot.fs.entity.dao.CallVoiceNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CcCallVoiceNotificationMapper {

    int insert(CallVoiceNotification record);
    

    int insertBatch(@Param("list") List<CallVoiceNotification> records);


    List<CallVoiceNotification> selectByTelephone(@Param("telephone") String telephone);

    List<CallVoiceNotification> selectByBatchId(@Param("batchId") String batchId);

    List<CallVoiceNotification> selectRetryCallPhones(@Param("maxRetry")int maxRetry,@Param("timeLimit") long timeLimit,@Param("validTime") long validTime);

    int resetHistoryData(@Param("currentTime") long currentTime, @Param("todayTime") long todayTime);

    int updateBatch(@Param("list") List<CallVoiceNotification> records);

    int deleteBatchByIds(@Param("ids") List<String> ids);
}
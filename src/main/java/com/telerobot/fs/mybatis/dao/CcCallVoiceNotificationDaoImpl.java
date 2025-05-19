package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.mybatis.persistence.CcCallVoiceNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CcCallVoiceNotificationDaoImpl implements CcCallVoiceNotificationDao {

    @Autowired
    private CcCallVoiceNotificationMapper mapper;

    @Override
    public int insert(CallVoiceNotification record) {
        return mapper.insert(record);
    }

    @Override
    public int insertBatch(List<CallVoiceNotification> records) {
        return mapper.insertBatch(records);
    }

    @Override
    public List<CallVoiceNotification> selectByTelephone(String telephone) {
        return mapper.selectByTelephone(telephone);
    }

    @Override
    public List<CallVoiceNotification> selectRetryCallPhones(int maxRetry, long timeLimit, long validTime) {
        return mapper.selectRetryCallPhones(maxRetry, timeLimit, validTime);
    }

    @Override
    public int resetHistoryData(long currentTime, long todayTime) {
        return mapper.resetHistoryData(currentTime, todayTime);
    }

    @Override
    public int updateBatch(List<CallVoiceNotification> records) {
        return mapper.updateBatch(records);
    }

    @Override
    public int deleteBatchByIds(List<String> ids) {
        return mapper.deleteBatchByIds(ids);
    }

    @Override
    public List<CallVoiceNotification> selectByBatchId(String batchId) {
        return mapper.selectByBatchId(batchId);
    }


}

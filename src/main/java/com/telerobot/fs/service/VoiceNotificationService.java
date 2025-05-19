package com.telerobot.fs.service;

import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.mybatis.dao.CcCallVoiceNotificationDao;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class VoiceNotificationService {

    private final Logger log = LoggerFactory.getLogger(SysService.class);

    @Autowired
    private CcCallVoiceNotificationDao callVoiceNotificationDao;

    public int insert(CallVoiceNotification record) {
        return callVoiceNotificationDao.insert(record);
    }

    public int insertBatch(List<CallVoiceNotification> records) {
        try {
            return callVoiceNotificationDao.insertBatch(records);
        }catch (Throwable e){
           log.error("insertBatch error: {} {}",
                   e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
        }
        return 0;
    }

    public List<CallVoiceNotification> selectByTelephone(String telephone) {
        return callVoiceNotificationDao.selectByTelephone(telephone);
    }

    public int updateBatch(List<CallVoiceNotification> records) {
        return callVoiceNotificationDao.updateBatch(records);
    }

    /**
     *  筛选出在时效范围内有效的可以重新外呼尝试的号码
     * @return
     */
    public List<CallVoiceNotification> selectRetryCallPhones() {
        int maxRetry = Integer.parseInt(SystemConfig.getValue("voice-notification-max-retry", "3"));
        long timeLimit =
                System.currentTimeMillis() -
                (Integer.parseInt(SystemConfig.getValue("voice-notification-retry-time-interval", "5")) * 60 * 1000);
        long validTime =  System.currentTimeMillis() -
                (Integer.parseInt(SystemConfig.getValue("voice-notification-max-valid-time-len", "60")) * 60 * 1000);
        return callVoiceNotificationDao.selectRetryCallPhones(maxRetry, timeLimit, validTime);
    }

    public int resetHistoryData() {
        long currentTime = System.currentTimeMillis();
        String todayString = DateUtils.formatDate(new Date());
        long todayTime = DateUtils.parseDate(todayString).getTime();
        return callVoiceNotificationDao.resetHistoryData(currentTime, todayTime);
    }

    public int deleteBatchByIds(List<String> ids) {
        try {
            return callVoiceNotificationDao.deleteBatchByIds(ids);
        }catch (Throwable e){
            return 0;
        }
    }

    public List<CallVoiceNotification> selectByBatchId(String batchId) {
        return callVoiceNotificationDao.selectByBatchId(batchId);
    }

}

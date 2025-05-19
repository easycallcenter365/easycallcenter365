package com.telerobot.fs.outbound.notification;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.service.VoiceNotificationService;
import com.telerobot.fs.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SQLQueue {
	private static  final Logger log = LoggerFactory.getLogger(SQLQueue.class);
	private static VoiceNotificationService dbService = AppContextProvider.getBean(VoiceNotificationService.class);
	private static final Object LOCKER = new Object();
	private static  List<CallVoiceNotification> phoneNumListForUpdate = new ArrayList<>(10000);

	/**
	 * 添加通话状态到SQL队列(修改记录)
	 * @param phone
	 */
	public static void addToSQLQueueForUpdate(CallVoiceNotification phone){
		synchronized (LOCKER) {
			phoneNumListForUpdate.add(phone);
		}
    }
	private static void addToSQLQueueForUpdate(List<CallVoiceNotification> list){
		synchronized (LOCKER) {
			phoneNumListForUpdate.addAll(list);
		}
	}


	static {
		new Thread(
				new Runnable() {
					@Override
					public void run() {
						ProcessSQLQueue();
					}
				} , "processSQLQueue").start();
	}

	private static void ProcessSQLQueue(){
		log.info("已经启动SQL队列线程!");
		while(true) {
			if (phoneNumListForUpdate.size() > 0) {
				List<CallVoiceNotification> _phoneNumListForUpdate = new ArrayList<CallVoiceNotification>(phoneNumListForUpdate.size() + 100);
 				synchronized (LOCKER) {
					// 处理修改队列
					int arrayLen = phoneNumListForUpdate.size();
					for (int i = arrayLen - 1; i >= 0; i--) {
						CallVoiceNotification phone = phoneNumListForUpdate.get(i);
						_phoneNumListForUpdate.add(phone);
					}
					phoneNumListForUpdate.clear();
				}


				long startTime = System.currentTimeMillis();
				int updateAffectRow = 0;
				try {
					updateAffectRow = dbService.updateBatch(_phoneNumListForUpdate);
				} catch (Exception e) {
					String updateList = JSON.toJSONString(_phoneNumListForUpdate);
					log.error("SQL队列修改数据执行失败：{}， 修改列表: {}",
							e.toString(), updateList);
				}
				if (updateAffectRow != 0) {
					long cost = System.currentTimeMillis() - startTime;
					if(updateAffectRow == _phoneNumListForUpdate.size()) {
						log.info("SQL队列执行了一次SQL批量修改，影响行数:{},耗时：{}毫秒. ",
								updateAffectRow, cost);
					}else{
						log.warn("SQL队列执行了一次SQL批量修改，预期影响行数:{}, 实际影响行数:{}, 耗时：{}毫秒. ",
								_phoneNumListForUpdate.size(), updateAffectRow, cost);
					}
				}
				_phoneNumListForUpdate.clear();
				_phoneNumListForUpdate = null;
			}
			ThreadUtil.sleep(12000);
		}
	}

}
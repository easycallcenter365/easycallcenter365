package com.telerobot.fs.wshandle;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.po.AgentEntity;
import com.telerobot.fs.entity.pojo.AgentStatus;
import com.telerobot.fs.mybatis.dao.SysDao;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
	private static SessionManager instance;
	private static final Object syncRoot = new Object();
	private static final Logger logger = LoggerFactory.getLogger(MessageHandlerEngine.class);
	
	/**
	 * 保存所有客户端的Session会话信息的容器
	 */
	private Map<String, SessionEntity> sessionContainer = new ConcurrentHashMap<String, SessionEntity>();
	
	
	private SessionManager() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				resetAgentBusyLockTime();
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				logger.info("websocket sessionManager thread has been started ...");
				while (true) {
					deleteAndGetInvalidSession();
					int timeout = Integer.parseInt(AppContextProvider.getEnvConfig("ws-server.ws-session-timeout").trim());
					ThreadUtil.sleep(timeout * 1000);
				}
			}
		}, "deleteInvalidSession").start();
	}

    private int lastSessionCount = 0;

	/**
	 * 删除过期无效的会话
	 * 
	 * @return
	 */
	private List<String> deleteAndGetInvalidSession() {
		List<String> invalidOptList = new ArrayList<String>(10);
		Iterator<Map.Entry<String, SessionEntity>> it = sessionContainer.entrySet().iterator();
		int sessionCount = 0;
        while(it.hasNext()){
            Map.Entry<String, SessionEntity> entry = it.next();
            SessionEntity tmpObj = entry.getValue();
            if (!tmpObj.IsValid()) {
				invalidOptList.add(tmpObj.getOpNum());
				MessageHandlerEngineList.getInstance().delete(tmpObj.getSessionId(), false);

				it.remove();
			}
			sessionCount ++;
        }
		if(lastSessionCount  != sessionCount) {
			logger.info("current session count: {}", sessionCount);
			lastSessionCount = sessionCount;
		}
		if (invalidOptList.size() != 0) {
				logger.info("Session {} has been Expired, delete  from SessionContainer.", CommonUtils.ListToString(invalidOptList, true));
		}
		removeOnlineUser(invalidOptList);
		return invalidOptList;
	}

	/**
	 * 根据工号删除在线用户;
	 * @param optList
	 * @return
	 */
	private int removeOnlineUser(List<String> optList){
		if(optList.size() == 0){
			return 0;
		}
		int affectRow = 0;
		try {
			affectRow = AppContextProvider.getBean(SysDao.class).removeOnlineUser(optList);
			logger.info("从数据库中删除会话过期用户数: {}", affectRow);
		}
		catch (Exception e){
			logger.error("从数据库中删除会话过期用户失败: {}", e.toString());
		}
		return affectRow;
	}

	/**
	 * 根据工号删除在线用户;
	 * @param opnum  工号
	 * @return
	 */
	private int removeOnlineUser(String opnum){
		ArrayList<String> optList = new ArrayList<>();
		optList.add(opnum);
		int affectRow = 0;
		try {
			affectRow = AppContextProvider.getBean(SysDao.class).removeOnlineUser(optList);
			logger.info("Delete the number of expired session users from the database: {}", affectRow);
		}
		catch (Exception e){
			logger.error("database error: {}", e.toString());
		}
		return affectRow;
	}

	private  int addOnlineUser(SessionEntity session){
		AgentEntity entity = new AgentEntity();
		entity.setId(UuidGenerator.GetOneUuid());
		entity.setClientIp(session.getClientIp());
		entity.setExtnum(session.getExtNum());
        entity.setOpnum(session.getOpNum());
		entity.setGroupId(session.getGroupId());
		entity.setLoginTime(session.getLoginTime());
		entity.setSessionId(session.getSessionId());
		entity.setSkillLevel(session.getSkillLevel());
        entity.setAgentStatus(AgentStatus.justLogin);
		entity.setSessionId(session.getSessionId());


		int affectRow = 0;
		try {
			affectRow = AppContextProvider.getBean(SysDao.class).addOnlineUser(entity);
			logger.info("addOnlineUser affectRow: {}", affectRow);
		}
		catch (Exception e){
			logger.error("addOnlineUser failed: {}", e.toString());
		}
		return affectRow;
	}

	/**
	 * 通过单体模式实现，返回一个SessionManager的实例
	 * 
	 * @return
	 */
	public static SessionManager getInstance() {
		if (instance == null) {
			synchronized (syncRoot) {
				if (instance == null) {
					instance = new SessionManager();
				}
			}
		}
		return instance;
	}

	/**
	 * 定时批量重置 cc_online_user表 座席锁定状态 busy_lock_time；
	 * 如果座席的锁定状态超过6秒仍然没有解除，说明电话转接可能出现异常或者其他原因导致，
	 * 此时如果不解除锁定状态，会导致座席永远处于忙碌状态而无法接到电话。
	 */
	private void resetAgentBusyLockTime() {
		logger.info("start resetAgentBusyLockTime thread.");
		int maxLooper = 6000;
		int counter = 0;

		while (true) {

			long timeout = System.currentTimeMillis() - 9 * 1000;
			List<SessionEntity> sessionList =  AppContextProvider.getBean(SysService.class).
					selectAgentBusyLockTimeout(timeout);

			if(null != sessionList && sessionList.size() > 0) {
				for (SessionEntity session : sessionList) {
					int affectRow = AppContextProvider.getBean(SysService.class).resetAgentBusyLockTimeEx(
							session.getOpNum(), timeout
					);
					if (affectRow > 0) {
						logger.info(" resetAgentBusyLockTimeEx affectRow:{}, opnum={}",
								affectRow, session.getOpNum());
					}

					MessageHandlerEngine engine = MessageHandlerEngineList.getInstance().
							getMsgHandlerEngineByOpNum(session.getOpNum());
					if (null != engine) {
						if (engine.getSessionInfo() != null) {
							engine.getSessionInfo().unLock();
						}
					}
				}
			}

			counter++;
			if(counter > maxLooper){
				counter = 0;
				logger.info("定时批量重置座席锁定状态 的线程运行中...");
			}
			ThreadUtil.sleep(1500);
		}
	}

	/**
	 * 根据SessionId更新会话的活跃时间
	 * @param sessionId
	 * @return
	 */
	public boolean updateSessionActiveTime(String sessionId) {
		SessionEntity  sessionEntity = this.sessionContainer.get(sessionId);
		if(sessionEntity != null){
			sessionEntity.setLastActiveTime(System.currentTimeMillis());
			return true;
		}
		return false;
	}

	/**
	 * 保存当前会话信息；(用户身份认证通过后才进行保存会话状态) 把当前客户端会话信息存入系统
	 * 
	 * @param clientSession
	 * @return 如果会话保存成功则返回空字符串，否则返回错误原因
	 */
	public boolean add(SessionEntity clientSession) {
		if (clientSession == null) {
			return false;
		}
		boolean found = false;
		String deleteClientId = "";

		Iterator<Map.Entry<String, SessionEntity>> it = sessionContainer.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, SessionEntity> entry = it.next();
            SessionEntity tmpObj = entry.getValue();
            if (tmpObj.getOpNum().equalsIgnoreCase(clientSession.getOpNum())) {
				found = true;
				deleteClientId = tmpObj.getSessionId();
				it.remove();
				break;
			}
        }

        //先从数据库中删除可能存在的指定工号的登录信息;
		removeOnlineUser(clientSession.getOpNum());

		this.sessionContainer.put(clientSession.getSessionId(), clientSession);
		addOnlineUser(clientSession);

		logger.info("Current Session count is  {}, successfully add a session object to SessionContainer. Details: {}",
				sessionContainer.size(),
				clientSession.toString());
		if (found) {
			MessageHandlerEngine engine = MessageHandlerEngineList.getInstance().getMsgHandlerEngine(deleteClientId);
			if (engine != null && !engine.getDisposeStatus()) {
				try {
					MessageResponse response = new MessageResponse();
					response.setMsg("user_logined_on_other_device");
					response.setStatus(201);
					engine.sendReplyToAgent(response);
				} catch (Exception e) {
					logger.info("send websocket msg error: {}", e.toString());
				}
				MessageHandlerEngineList.getInstance().delete(deleteClientId, false);
			}
		}
		return true;
	}

	/**
	 * 把当前客户端会话信息从系统中删除
	 * 
	 * @param sessionId
	 *            客户端会话的SessionId
	 * @return
	 */
	public boolean delete(String sessionId) {
		if (StringUtils.isNullOrEmpty(sessionId)) {
			return true;
		}
		boolean found = false;
		SessionEntity tmpObj = sessionContainer.get(sessionId);
		if (tmpObj != null) {
			removeOnlineUser(tmpObj.getOpNum());
			this.sessionContainer.remove(sessionId);
			found = true;
		}
		return found;
	}
}
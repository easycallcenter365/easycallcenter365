package com.telerobot.fs.wshandle;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.telerobot.fs.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandlerEngineList {
	private MessageHandlerEngineList() {
	}

	private static final Logger logger = LoggerFactory.getLogger(MessageHandlerEngineList.class);
	private static MessageHandlerEngineList instance = null;
	private static final Object syncRoot = new Object();

	/**
	 * 存放每个客户端的MessageHandlerEngine会话对象的集合
	 */
	private Map<String, MessageHandlerEngine> msgHandlerEngineList = new ConcurrentHashMap<String, MessageHandlerEngine>();
	
	public Map<String, MessageHandlerEngine> getList(){
		return msgHandlerEngineList;
	}
	
	public int size(){
		return msgHandlerEngineList.size();
	}
	
	public MessageHandlerEngine get(String key){
		return getMsgHandlerEngine(key);
	}
	
	/**
	 * 获取指定clientId的ChannelHandlerContext
	 * @param key
	 * @return
	 */
	public ChannelHandlerContext getCtx(String key){
		MessageHandlerEngine engine = getMsgHandlerEngine(key);
		if(engine != null){
			return engine.clientSocketSession;
		}
		return null;
	}
	
	/**
	 * 通过单体模式实现，返回一个MsgHandlerEngineList的实例
	 * 
	 * @return
	 */
	public static MessageHandlerEngineList getInstance() {
		if (instance == null) {
			synchronized (syncRoot) {
				if (instance == null) {
					instance = new MessageHandlerEngineList();
				}
			}
		}
		return instance;
	}

	/**
	 * 通过工号OpNum获取MessageHandlerEngine对象
	 *
	 * @param opNum 工号
	 * @return
	 */
	public MessageHandlerEngine getMsgHandlerEngineByOpNum(String opNum) {
		if(StringUtils.isNullOrEmpty(opNum)) {
			return null;
		}
		Iterator<Map.Entry<String, MessageHandlerEngine>> it = instance.msgHandlerEngineList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, MessageHandlerEngine> entry = it.next();
			MessageHandlerEngine engine = entry.getValue();
			SessionEntity sessionEntity = engine.getSessionInfo();
			if(null != sessionEntity) {
				if (sessionEntity.IsValid() &&  opNum.equals(sessionEntity.getOpNum())) {
					return engine;
				}
			}
		}
		return null;
	}

	/**
	 * 通过分机号extNum获取MessageHandlerEngine对象
	 *
	 * @param extNum 分机号
	 * @return
	 */
	public MessageHandlerEngine getMsgHandlerEngineByExtNum(String extNum) {
		if(StringUtils.isNullOrEmpty(extNum)) {
			return null;
		}
		Iterator<Map.Entry<String, MessageHandlerEngine>> it = instance.msgHandlerEngineList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, MessageHandlerEngine> entry = it.next();
			MessageHandlerEngine engine = entry.getValue();
			SessionEntity sessionEntity = engine.getSessionInfo();
			if(null != sessionEntity) {
				if (sessionEntity.IsValid() &&  extNum.equals(sessionEntity.getExtNum())) {
					return engine;
				}
			}
		}
		return null;
	}


	/**
	 * 通过clientID获取MessageHandlerEngine对象
	 * 
	 * @param clientID
	 * @return
	 */
	public MessageHandlerEngine getMsgHandlerEngine(String clientID) {
		try {
			MessageHandlerEngine engine = msgHandlerEngineList.get(clientID);
			if (engine != null && !engine.getDisposeStatus()) {
				return engine;
			}
		} catch (Exception e) {
			logger.debug("GetMsgHandlerEngine_ByClientID error! Details: {}", e.toString());
		}
		return null;
	}

	/**
	 * 销毁指定clientID 的 MessageHandlerEngine对象
	 * 
	 * @param clientID
	 * @param deleteSession
	 */
	public boolean delete(String clientID, boolean deleteSession) {
		MessageHandlerEngine destEngine = msgHandlerEngineList.get(clientID);
		if (destEngine != null) {
			msgHandlerEngineList.remove(clientID);
			if (destEngine.getSessionInfo() != null) {
				logger.info("try to destroy session: " + destEngine.getSessionInfo().toString());
			}
		}

		// 防止访问已经释放的对象
		if (destEngine != null && !destEngine.getDisposeStatus())
		{
			try {
				destEngine.clientSocketSession.close();
				// 关闭WebSocket连接
			} catch (Exception e) {
			}
			destEngine.dispose();
			logger.info("remove an object from msgHandlerEngineList,clientID is {}" , clientID);
		}

		if (deleteSession) {
			SessionManager.getInstance().delete(clientID);
		}
		return true;
	}

	/**
	 * 添加一个客户端的MessageHandlerEngine对象
	 * 
	 * @param mySocketObj
	 * @return
	 */
	public boolean add(MessageHandlerEngine mySocketObj) {
		if (mySocketObj == null) {
			return false;
		}
		try {
			msgHandlerEngineList.put(mySocketObj.getClientSessionID(), mySocketObj);
			return true;
		} catch (Exception ex) {
			logger.info("添加MessageHandlerEngine对象到系统列表时出错，客户端id是: {}", mySocketObj.getClientSessionID());
			return false;
		}
	}

	/**
	 * 发送广播消息给所有在线用户
	 * @param msg
	 */
	public static void doBroadcast(MessageResponse msg, String groupId){
		if(instance != null) {
			Iterator<Map.Entry<String, MessageHandlerEngine>> it = instance.msgHandlerEngineList.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, MessageHandlerEngine> entry = it.next();
				MessageHandlerEngine engine = entry.getValue();
				SessionEntity sessionEntity = engine.getSessionInfo();
				if(null != sessionEntity) {
					if (sessionEntity.IsValid() &&  groupId.equals(sessionEntity.getGroupId())) {
						engine.sendReplyToAgent(msg);
					}
				}
			}
		}
	}
}

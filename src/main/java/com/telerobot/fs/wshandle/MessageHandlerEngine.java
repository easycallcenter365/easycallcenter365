package com.telerobot.fs.wshandle;

import com.telerobot.fs.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageHandlerEngine   {
	private static final Logger logger = LoggerFactory.getLogger(MessageHandlerEngine.class);

	public boolean checkAuth(){
		if (this.getSessionInfo() == null ||
				!this.getSessionInfo().IsValid()) {
			String tips = "You are not logged in or your login timed out, cant not process your request.";
			logger.info("{} {} ", getTraceId(), tips);
			sendReplyToAgent(new MessageResponse(RespStatus.REQUEST_PARAM_ERROR,  tips));
			return false;
		}
		return true;
	}

	/**
	 * 客户端Socket连接对象
	 */
	public ChannelHandlerContext clientSocketSession = null;

	/**
	 * 客户端的Socket会话编号
	 */
	public String getClientSessionID() {
		return clientSocketSession.channel().id().asLongText();
	}


	public SessionEntity getSessionInfo() {
		return this.sessionInfo;
	}

	private String traceId = "";
	/**
	 *  获取用于日志打印的traceId
	 *  （这里使用分机号）
	 * @return
	 */
	public String getTraceId(){
		if(StringUtils.isNullOrEmpty(traceId)){
			traceId = this.getSessionInfo().getOpNum() + "-" + this.getSessionInfo().getExtNum() + ":";
		}
		return traceId;
	}

	/**
	 * 保存当前客户端的 工号分机号等会话信息的Session对象
	 */
	private SessionEntity sessionInfo = null;

	/**
	 * 初始化当前MsgHandlerEngine对象的SessionInfo信息
	 */
	public boolean initSession(SessionEntity _sessionInfo) {
		this.sessionInfo = _sessionInfo;
		return true;
	}

	/**
	 * 创建一个map容器保存当前引擎实例动态创建的msgHander对象，避免每次都要创建，以提高效率
	 */
	private Map<String, IMsgHandler> msgHanderList = new HashMap<String, IMsgHandler>(10);

	/**
	 * 锁助手，用于在生成当前Client信息处理对象时，执行锁定，避免产生多个相同的信息处理对象
	 * 注意这里不能使用static关键字，因为只锁定当前Client；
	 */
	private Object lockHelper = new Object();

	/**
	 * 从msgHanderList中获取MsgHandler
	 * 
	 * @param _moduleName
	 * @return
	 */
	private IMsgHandler GetServices(String _moduleName) {
		IMsgHandler handler = msgHanderList.get(_moduleName);
		if(handler != null && !handler.getIsDisposed()){
		   return handler;
		}
		return null;
	}

	private IMsgHandler getMsgHandler(MsgActionType msgTypeObj) {
		IMsgHandler currentHandler = null;
		currentHandler = GetServices(msgTypeObj.getName());
		if (currentHandler == null) {
			// 采用双重检测机制，防止重复创建
			synchronized (lockHelper)
			{
				currentHandler = GetServices(msgTypeObj.getName());
				if (currentHandler != null) {
					return currentHandler;
				}

				try {
					currentHandler = (IMsgHandler) (Class.forName(msgTypeObj.getClassFullName()).newInstance());
				} catch (Exception e) {
					logger.error(e.toString());
				}
				if (currentHandler == null) {
					return null;
				}

				currentHandler.activeCurrentObject(this);
				msgHanderList.put(msgTypeObj.getName(), currentHandler);
			}
		}
		return currentHandler;
	}

	/**
	 * 获取指定“消息响应处理类型的”IMsgHandler对象
	 * 
	 * @param msgAction
	 *            消息响应处理类型，比如“TalkToFsServer”
	 * @return
	 */
	public IMsgHandler getMsgHandler(String msgAction) {
		MsgActionType msgTypeObj = MsgActionTypeUtil.GetActionTypeByName(msgAction);
		if (msgTypeObj != null) {
			return getMsgHandler(msgTypeObj);
		}
		return null;
	}

	/**
	 * 移除指定名称的MsgHandler对象
	 * 
	 * @param handlerName
	 * @return
	 */
	public boolean removeMsgHandler(String handlerName) {
		synchronized (lockHelper) {
			if (this.msgHanderList.containsKey(handlerName)) {
				this.msgHanderList.remove(handlerName);
				logger.debug("从msgHanderList中查询到 {} 对象，已经移除.", handlerName);
			}
		}
		return true;
	}

	public IMsgHandler  getMessageHandleByName(String handleName){
		MsgActionType msgTypeObj = MsgActionTypeUtil.GetActionTypeByName(handleName);
		if (msgTypeObj != null) {
			return getMsgHandler(msgTypeObj);
		}
		return null;
	}

	public void processMsg(final MsgStruct msg) {
		if (StringUtils.isNullOrEmpty(msg.getAction())) {
			return;
		}
		MsgActionType msgTypeObj = MsgActionTypeUtil.GetActionTypeByName(msg.getAction());
		if (msgTypeObj != null) {
			IMsgHandler currentHandler = getMsgHandler(msgTypeObj);
			if (currentHandler == null) {
				logger.error("{} Error! Can not load type: {}", this.getTraceId(), msgTypeObj.getClassFullName());
				sendReplyToAgent(new MessageResponse(RespStatus.SERVER_ERROR, "cant not load module " + msgTypeObj.getName(), null));
			} else {
				  if(!msg.getAction().equalsIgnoreCase("setHearBeat")) {
					logger.info("{} recv msg: {} ; handler is: {} " , this.getTraceId(), msg.toString() ,msgTypeObj.getClassFullName());
				 }
				currentHandler.processTask(msg);
			}
		} else {
			sendReplyToAgent(new MessageResponse(RespStatus.REQUEST_PARAM_ERROR, "method_not_exists", null));
		}
	}

	/**
	 * 向客户端发送消息
	 * 
	 * @param msg
	 */
    public void sendReplyToAgent(MessageResponse msg) {
		try {
			clientSocketSession.writeAndFlush(new TextWebSocketFrame(msg.toString()));
			logger.info("{} Send response to client: {}",  this.getTraceId(), msg.toString());
		} catch (Exception ex) {
			logger.error("{} SendReplyToAgent error, msg:{}, details: {} " , this.getTraceId(),  msg, ex.toString());
		}
	}


	/**
	 * 获取一个值，判断对象是否已经销毁
	 * 
	 * @return
	 */
	public boolean getDisposeStatus() {
		return disposed;
	}

	private boolean disposed = false;
	private Object disposeLocker = new Object();

	public void dispose() {
		synchronized (disposeLocker) {
			if (!this.disposed) {
				disposed = true;
			}
			for (IMsgHandler handler : msgHanderList.values()) {
				handler.dispose();
			}
			msgHanderList.clear();
			msgHanderList = null;
			logger.info("{} MsgEngine is going to destroyed...", this.getTraceId());
		}
	}

	public MessageHandlerEngine(ChannelHandlerContext clientSession) {
		this.clientSocketSession = clientSession;
	}

	public boolean getIsDisposed() {
		return disposed;
	}

}

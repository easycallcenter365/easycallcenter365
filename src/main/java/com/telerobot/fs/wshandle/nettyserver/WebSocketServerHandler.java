package com.telerobot.fs.wshandle.nettyserver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.entity.dao.BizGroup;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadUtil;
import com.telerobot.fs.wshandle.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * websocket 具体业务处理方法
 *
 * @author DELL
 */

@Component
@Sharable
public class WebSocketServerHandler extends BaseWebSocketServerHandler {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

	/**
	 * 当客户端连接成功，返回个成功信息
	 */
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {}

	/**
	 * 当客户端断开连接
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// 从连接池内剔除
        String clientId = ctx.channel().id().asLongText();
        logger.debug("client disconnected:{}", clientId);
		WebsocketThreadPool.addTask(new Runnable() {
			@Override
			public void run() {
				MessageHandlerEngine engine =  MessageHandlerEngineList.getInstance().getMsgHandlerEngine(clientId);
				if(engine != null){
					InactiveNotice.onDisconnected(engine.getSessionInfo());
				}
			}
		});
        MessageHandlerEngineList.getInstance().delete(clientId, true);
        ctx.close();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// http：//xxxx
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			// ws://xxxx
			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
		// channelRead0 不需要显式释放msg; (丢弃已接收的消息)
		// 如果放入到线程池去处理msg， 需要显式释放这个msg;
	}

	// @Override
	// public void channelRead(ChannelHandlerContext ctx, Object msg) {
	// ReferenceCountUtil.release(msg);
	// }

	public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		// 关闭请求
		if (frame instanceof CloseWebSocketFrame) {
			WebSocketServerHandshaker handshaker = Constant.handShakerMap.get(ctx.channel().id().asLongText());
			if (handshaker != null) {
				handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			}
			return;
		}
		// ping请求
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		// 只支持文本格式，不支持二进制消息
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new Exception("only plain-text format supported.");
		}

		// 客服端发送过来的消息
		final String msg = ((TextWebSocketFrame) frame).text();
		logger.debug("current connections：{}", MessageHandlerEngineList.getInstance().size());
		WebsocketThreadPool.addTask(new Runnable() {
			@Override
			public void run() {
				String clientID = ctx.channel().id().asLongText();
				logger.debug(String.format("receive message: %s, from: %s , clientId: %s .", msg,
						ctx.channel().remoteAddress().toString(), clientID));
				MessageHandlerEngine msgEngine = MessageHandlerEngineList.getInstance()
						.getMsgHandlerEngine(clientID);
				int trycount = 0;
				int maxtry = 1000;
				long startTime = System.currentTimeMillis();
				while (msgEngine == null) {
					ThreadUtil.sleep(5);
					trycount += 1;
					msgEngine = MessageHandlerEngineList.getInstance().getMsgHandlerEngine(clientID);
					if (msgEngine != null) {
						int spendSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
						logger.debug("successfully get messageHandlerEngine object, spend seconds : {}" , spendSeconds);
						break;
					}
					if (trycount > maxtry) {
						break;
					}
				}

				if (msgEngine == null) {
					MessageResponse replyMsg = new MessageResponse();
					replyMsg.setMsg("server too busy, can't get msgEngine");
					replyMsg.setStatus(500);
					ctx.writeAndFlush(new TextWebSocketFrame(replyMsg.toString()));
					ctx.close();
					logger.error("{} server too busy, can't get msgEngine.", clientID);
					return;
				}

				MsgStruct msgObj = null;
				try {
					msgObj = JSON.parseObject(msg, MsgStruct.class);
				} catch (Exception e) {
					sendReplyToAgent(400, "invalid json format.", msgEngine);
					return;
				}
				if (msgObj == null) {
					sendReplyToAgent(400, "operation not supported", msgEngine);
					return;
				}
				boolean notHasHeader = StringUtils.isNullOrEmpty(msgObj.getAction());
				boolean notHasBody = StringUtils.isNullOrEmpty(msgObj.getBody());
				if (notHasHeader || notHasBody) {
					sendReplyToAgent(400, "except both 'action' and 'body' in request msg.", msgEngine);
					return;
				}

				if(!msgEngine.checkAuth()){
					return;
				}

                if(!"setHearBeat".equals(msgObj.getAction())){
                    msgEngine.processMsg(msgObj);
                }else{
                    // 心跳
                    logger.info("{} recv websocket client heartBeat: {}", msgEngine.getTraceId(),  msgEngine.getClientSessionID() );
					//每次消息接收，都更新下用户活动时间;
					SessionEntity sessionInfo = msgEngine.getSessionInfo();
					if (sessionInfo != null) {
						sessionInfo.setLastActiveTime(System.currentTimeMillis());
					}
                }
			}
		});
	}
	
	private void sendReplyToAgent(int statusCode, String msg, MessageHandlerEngine messageHandlerEngine){
		MessageResponse replyMsg = new MessageResponse();
		replyMsg.setMsg(msg);
		replyMsg.setStatus(statusCode);
		messageHandlerEngine.sendReplyToAgent(replyMsg);
	}

	/**
	 * 第一次请求是http请求，请求头包括ws的信息
	 * @param ctx
	 * @param req
	 */
	public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		if (!req.decoderResult().isSuccess()) {
			sendHttpResponse(ctx, req,
					new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		WebSocketServerHandshaker handshaker = Constant.handShakerMap.get(ctx.channel().id().asLongText());
		if (handshaker == null) {
			String wsuri = "ws://127.0.0.1:1081"  + req.uri();
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(wsuri, null, true);
			handshaker = wsFactory.newHandshaker(req);
			Constant.handShakerMap.put(ctx.channel().id().asLongText(), handshaker);
			// 在这里处理用户登录;
			handleWsLogin(ctx, req.uri());
		}
		if (handshaker == null) {
			// 不支持
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}
	}

	private void handleWsLogin(ChannelHandlerContext ctx, final String requestURI) {
		WebsocketThreadPool.addTask(new Runnable() {
			@Override
			public void run() {
				logger.debug(String.format("websocket OnOpen, getId:%s , remoteAddress: %s.",
						ctx.channel().id().asLongText(), ctx.channel().remoteAddress().toString()));
				MessageResponse replyMsg = new MessageResponse();
				String queryString = requestURI;
				if (!queryString.contains("?")) {
					queryString = "";
				} else {
					String[] tmpArray = queryString.split("\\?");
					if (tmpArray.length == 2) {
						queryString = tmpArray[1];
					}
				}

				Map<String, String> params = CommonUtils.processRequestParameter(queryString);
				String token = params.get("loginToken");
				logger.info("{} recv login request.", token);
				Map<String, String> loginMap = CommonUtils.validateToken(token, ""); // Boolean.parseBoolean("true");
				if (null == loginMap) {
					replyMsg.setStatus(400);
					replyMsg.setMsg("token verify failed.");
					ctx.writeAndFlush(new TextWebSocketFrame(replyMsg.toString()));
					ctx.close();
				} else {
					String extnum = loginMap.get("extnum");
					String opnum = loginMap.get("opnum");
					String skillLevel = loginMap.get("skillLevel");
					String groupId = loginMap.get("groupId");
					String tips = String.format("successfully decode loginToken, extnum=%s, opnum=%s, skillLevel=%s, groupId=%s",
							 extnum, opnum, skillLevel, groupId
					);
					logger.info(tips);
					if (StringUtils.isNullOrEmpty(extnum) ||
							StringUtils.isNullOrEmpty(opnum) ||
							StringUtils.isNullOrEmpty(skillLevel) ||
							StringUtils.isNullOrEmpty(groupId) ) {
						replyMsg.setStatus(400);
						replyMsg.setMsg(tips + " ; parameter missing,  (extnum、 opnum、 skillLevel、groupId) 至少有一个为空... ");
						ctx.writeAndFlush(new TextWebSocketFrame(replyMsg.toString()));
						ctx.close();
						return;
					}
					String traceId = String.format("%s-%s:", opnum, extnum);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("extnum", extnum);
					jsonObject.put("opnum", opnum);
					jsonObject.put("groupId", groupId);
					List<BizGroup> groups = AppContextProvider.getBean(SysService.class).getAllGroupList();
					jsonObject.put("groups", groups);
					replyMsg.setStatus(200);
					replyMsg.setObject(jsonObject);
					SessionEntity sessionEntity = new SessionEntity();
					sessionEntity.setClientIp(ctx.channel().remoteAddress().toString());
					sessionEntity.setExtNum(extnum);
					sessionEntity.setOpNum(opnum);
					sessionEntity.setSessionId(ctx.channel().id().asLongText());
					sessionEntity.setLastActiveTime(System.currentTimeMillis());
					sessionEntity.setSkillLevel(Integer.parseInt(skillLevel));
					sessionEntity.setLoginTime(System.currentTimeMillis());
					sessionEntity.setGroupId(groupId);
					boolean addSessionOk = SessionManager.getInstance().add(sessionEntity); // 添加到会话管理
					if (addSessionOk) {
						logger.info("{} successfully add current session.", traceId);
					} else {
						logger.error("{} failed to add current session.", traceId);
					}
					MessageHandlerEngine myEngine = new MessageHandlerEngine(ctx);
					logger.info("{} successfully create MsgEngine for current user.", traceId);
					if (!MessageHandlerEngineList.getInstance().add(myEngine)) {
						logger.error("{} failed to add MsgEngine to SysList.", traceId);
					}
					myEngine.initSession(sessionEntity);// 初始化session信息
					myEngine.sendReplyToAgent(replyMsg);
				}
			}
		});
	}

	public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static boolean isKeepAlive(FullHttpRequest req) {
		return false;
	}

	// 异常处理，netty默认是关闭channel
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof  java.io.IOException) {
			logger.info("netty IOException: {}", cause.toString());
		}else{
			logger.error("netty exceptionCaught: {}", cause.toString());
		}
		MessageHandlerEngineList.getInstance().delete(ctx.channel().id().asLongText(), true);
		ctx.close();
	}

}

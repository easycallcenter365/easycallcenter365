package com.telerobot.fs.wshandle.impl;


import com.telerobot.fs.wshandle.IMsgHandlerInitializer;
import com.telerobot.fs.wshandle.MessageHandlerEngine;
import com.telerobot.fs.wshandle.MsgHandlerBase;
import com.telerobot.fs.wshandle.MsgStruct;

/**
 * handlerWebSocketFrame 中已经处理心跳；无需再单独使用一个handler去处理心跳包;
 * (该类可以作为一个demo，演示如何实现一个MsgHandlerBase的子类;)
 * @author easycallcenter365@gmail.com
 */
@Deprecated
public class SetHearBeat extends MsgHandlerBase {

	@Override
	public void processTask(MsgStruct data) {
		 if(this.getSessionInfo() != null){
		    this.getSessionInfo().setLastActiveTime(System.currentTimeMillis());
			//logger.info("更新了会话活跃时间." + this.getSessionInfo().toString());
		 }
	}

	@Override
	public void activeCurrentObject(MessageHandlerEngine msgHandlerEngine, IMsgHandlerInitializer... initializer) {
		super.activeCurrentObject(msgHandlerEngine);
	}

}

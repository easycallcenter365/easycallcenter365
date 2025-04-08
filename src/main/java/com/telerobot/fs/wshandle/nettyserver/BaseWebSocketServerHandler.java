package com.telerobot.fs.wshandle.nettyserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;


/**
 * 发消息方式
 * 
 * */
public abstract class BaseWebSocketServerHandler extends SimpleChannelInboundHandler<Object>{

	
	/**
	 * 推送单个
	 * 
	 * */
	public static final void push(final ChannelHandlerContext ctx,final String message){
		
		TextWebSocketFrame tws = new TextWebSocketFrame(message);
		ctx.channel().writeAndFlush(tws);
	}
	/**
	 * 群发
	 * 
	 * */
	public static final void push(final ChannelGroup ctxGroup,final String message){
		TextWebSocketFrame tws = new TextWebSocketFrame(message);
		ctxGroup.writeAndFlush(tws);
	}
}

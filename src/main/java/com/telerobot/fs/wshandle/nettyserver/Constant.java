package com.telerobot.fs.wshandle.nettyserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
public class Constant {
	public static Map<String,WebSocketServerHandshaker> handShakerMap = new ConcurrentHashMap<String, WebSocketServerHandshaker>();
}

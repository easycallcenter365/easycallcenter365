package com.telerobot.fs.wshandle.nettyserver;

import com.telerobot.fs.config.AppContextProvider;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

 
public class WebSocketChildChannelHandler extends ChannelInitializer<SocketChannel>{

	private ChannelHandler webSocketServerHandler = AppContextProvider.getBean("webSocketServerHandler");

	public WebSocketChildChannelHandler() {
	}

	@SuppressWarnings("restriction")
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast("http-codec", new HttpServerCodec());
		ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
		ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
		ch.pipeline().addLast("handler",webSocketServerHandler);
	}

}

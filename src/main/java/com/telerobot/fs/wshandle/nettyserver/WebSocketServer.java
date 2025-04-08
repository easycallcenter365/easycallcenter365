package com.telerobot.fs.wshandle.nettyserver;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.mybatis.dao.SysDao;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * 启动服务
 * @author easycallcenter365@gmail.com
 */

@Component
@DependsOn({"appContextProvider", "myStartupRunner"})
public class WebSocketServer implements ApplicationListener<ApplicationReadyEvent> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
	/**
	 *  用于客户端连接请求
	 */
	private EventLoopGroup bossGroup;

	/**
	 * 用于处理客户端I/O操作
	 */
	private EventLoopGroup workerGroup;
	/**
	 *  服务端口
	 */
	private int port = 1081;

	/**
	 * 服务器的辅助启动类
	 */
	private ServerBootstrap serverBootstrap;

	private ChannelFuture channelFuture;

	public WebSocketServer() {
        logger.info("netty init...");
	}

	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public void setBossGroup(EventLoopGroup bossGroup) {
		this.bossGroup = bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public void setWorkerGroup(EventLoopGroup workerGroup) {
		this.workerGroup = workerGroup;
	}

	public ServerBootstrap getServerBootstrap() {
		return serverBootstrap;
	}

	public void setServerBootstrap(ServerBootstrap serverBootstrap) {
		this.serverBootstrap = serverBootstrap;
	}

	public ChannelFuture getChannelFuture() {
		return channelFuture;
	}

	public void setChannelFuture(ChannelFuture channelFuture) {
		this.channelFuture = channelFuture;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void runWebsocketServer() {
		try {
			this.port = Integer.parseInt(SystemConfig.getValue("ws-server-port", "1081"));
			try {
				AppContextProvider.getBean(SysDao.class).removeAllOnlineuser();
			}
			catch (Exception ex){
				logger.error("清空数据表agent_ex时发生错误: {}", ex.toString());
			}
			bulid(port);
		} catch (Exception e) {
            logger.info("netty 端口绑定失败: {}", e.toString());
		}
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent arg0) {
		logger.info("try to start netty websocket server...");
		this.serverBootstrap = new io.netty.bootstrap.ServerBootstrap();
		new Thread(new Runnable() {
			@Override
			public void run() {
				runWebsocketServer();
			}
		}).start();
	}

	private static boolean isLinux() {
		if (System.getProperty("os.name").toLowerCase().indexOf("linux") != -1) {
            return true;
        }
		return false;
	}

	public void bulid(int port) throws Exception {
		if (isLinux()) {
			bossGroup = new EpollEventLoopGroup();
			workerGroup = new EpollEventLoopGroup();
            logger.info("linux os ， using EpollEventLoopGroup");
		} else {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();
		}
		try {

			// （1）boss辅助客户端的tcp连接请求 worker负责与客户端之前的读写操作
			// （2）配置客户端的channel类型
			// (3)配置TCP参数，握手字符串长度设置
			// (4)TCP_NODELAY是一种算法，为了充分利用带宽，尽可能发送大块数据，减少充斥的小块数据，true是关闭，可以保持高实时性,若开启，减少交互次数，但是时效性相对无法保证
			// (5)开启心跳包活机制，就是客户端、服务端建立连接处于ESTABLISHED状态，超过2小时没有交流，机制会被启动
			// (6)netty提供了2种接受缓存区分配器，FixedRecvByteBufAllocator是固定长度，但是拓展，AdaptiveRecvByteBufAllocator动态长度
			// (7)绑定I/O事件的处理类,WebSocketChildChannelHandler中定义
			serverBootstrap.group(bossGroup, workerGroup);
			if (isLinux()) {
				serverBootstrap.channel(EpollServerSocketChannel.class);
				logger.info("linux os,  using EpollServerSocketChannel");
			} else {
				serverBootstrap.channel(NioServerSocketChannel.class);
			}
            logger.info(String.format("netty websocketserver port: %d ", port));
			ChannelHandler childChannelHandler = new WebSocketChildChannelHandler();
			serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
				 	// .option(ChannelOption.TCP_NODELAY, true)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(592048))
					.childHandler(childChannelHandler);
			//Netty4使用对象池，重用缓冲区
			serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			logger.info("netty websocket server started successfully...");
			channelFuture = serverBootstrap.bind(port).sync();
			channelFuture.channel().closeFuture().sync();
		} catch (Exception e) {
            logger.info("netty启动失败,jvm即将退出：{}", e.toString());
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			System.exit(0);
		}
	}

	// 执行之后关闭
	@PreDestroy
	public void close() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();

	}
}

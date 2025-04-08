package com.telerobot.fs.wshandle;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketThreadPool {
 
	/**
	 * 向线程池添加任务
	 * @param task
	 */
	public static void addTask(Runnable task){
		initThreadPool();
		executorService.execute(task);
	}

	private static final Logger logger = LoggerFactory.getLogger(WebsocketThreadPool.class);
	private static ThreadPoolExecutor executorService = null;
	private static final Object lockHelperThreadPool = new Object();
	public static class MyRejectPolicy implements RejectedExecutionHandler {
		private final Object lockHelper = new Object();
		private int rejectTaskCount = 0;
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			synchronized (lockHelper) {
				rejectTaskCount = rejectTaskCount + 1;
			}
			logger.error( "Netty Websocket server current rejected task count is : {}", rejectTaskCount);
		}
	}
	private static void initThreadPool() {
		if (executorService == null) {
			synchronized (lockHelperThreadPool) {
				if (executorService == null) {
					int wsMaxThreadsNum = Integer
							.parseInt(SystemConfig.getValue("max-agent-number").trim());
					logger.info("websocket maxThreadsNum: {}", wsMaxThreadsNum);
					if (wsMaxThreadsNum == 0) {
						logger.error("check ws_max_threads_num param !");
						return;
					}
					executorService = new ThreadPoolExecutor(wsMaxThreadsNum, wsMaxThreadsNum + 1, 900L, TimeUnit.SECONDS,
							new ArrayBlockingQueue<Runnable>(wsMaxThreadsNum));
					executorService.setRejectedExecutionHandler(new MyRejectPolicy());
				}
			}
		}
	}
}

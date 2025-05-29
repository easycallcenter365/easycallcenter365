package com.telerobot.fs.wshandle;

import com.telerobot.fs.wshandle.impl.PollAgentStatusList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 客户端消息解析处理类的抽象类
 * @date 2018年11月5日 上午10:08:31
 */
public abstract class MsgHandlerBase implements IMsgHandler {

 	protected static final Logger logger = LoggerFactory.getLogger(MsgHandlerBase.class);

    @Override
	public String getTraceId(){
        return msgHandlerEngine.getTraceId();
    }

	/**
	 * 消息容器(待处理的消息)
	 */
	protected ArrayBlockingQueue<String> messageQueueToSend = new ArrayBlockingQueue<String>(200);

	/**
	 * 激活当前对象时，必须要加锁，防止重复激活
	 */
	private Object activeLocker = new Object();

	/**
	 * 激活当前对象时，判断对象是否已经激活的信号
	 */
	private boolean isActived;

	/**
	 *  具体实现类 handler 的初始化和销毁方法类的引用;
	 *  相当于是一个中间人;
	 */
	protected IMsgHandlerInitializer initializer = null;


	protected MessageHandlerEngine msgHandlerEngine;

	@Override
	public boolean getIsDisposed() {
		return disposed;
	}

	/**
	 * 对象已经销毁的标志
	 */
	private boolean disposed = false;
	private Object disposeLocker = new Object();

	@Override
	public void dispose() {
		if (!disposed) {
			synchronized (disposeLocker) {
				if (!disposed) {
					disposed = true;
					try{
						if(null !=  initializer) {
							// 调用子类的销毁方法;
							initializer.destroyHandlerInstance();
						}
					}catch (Exception e){
						logger.error("{} Error occurred on destroyHandlerInstance: {}", getTraceId(),  e.toString());
					}
					if(this.messageQueueToSend != null){
						this.messageQueueToSend.clear();
					}
					logger.info("{} MsgHandlerBase destroyed... ", getTraceId());
				}
			}
		}
	}

	@Override
	public abstract void processTask(MsgStruct data);

	@Override
	public  void activeCurrentObject(MessageHandlerEngine msgHandlerEngine, IMsgHandlerInitializer... initializer){
		synchronized (this.activeLocker) {
			if (this.isActived) {
				return;
			}
			if(initializer.length != 0) {
				try {
					// 调用子类的初始化方法;
					initializer[0].activeCurrentHandlerInstance();
				}catch (Exception e){
					logger.error("{} error occurred on activeCurrentHandlerInstance: {}" , getTraceId(), e.toString());
				}
				this.initializer = initializer[0];
			}
			this.msgHandlerEngine = msgHandlerEngine;
			this.isActived = true;
		}
	}
	/**
  * 向坐席端发送回复
	 * 
	 * @param msg
	 */
	@Override
	public void sendReplyToAgent(MessageResponse msg) {
		this.msgHandlerEngine.sendReplyToAgent(msg);
	}

	/**
	 * 获取当前客户端的 工号分机号等会话信息的Session对象
	 */
	@Override
	public   SessionEntity getSessionInfo(){
		return msgHandlerEngine.getSessionInfo();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()){ return false;}
		if(this.getSessionInfo() == null) { return  false; }
		MsgHandlerBase that = (MsgHandlerBase) o;
		if(that.getSessionInfo() == null) { return  false; }
		return  this.getSessionInfo().getOpNum().equalsIgnoreCase(
				that.getSessionInfo().getOpNum())
				&&
				this.getSessionInfo().getExtNum().equalsIgnoreCase(
						that.getSessionInfo().getExtNum());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				getSessionInfo().getOpNum()  +
						 getSessionInfo().getExtNum()
		);
	}

}

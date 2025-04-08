package com.telerobot.fs.wshandle;


public interface IMsgHandler
    {
        String getTraceId();

        SessionEntity getSessionInfo();

       /**
        * 处理客户端的任务请求;
        * @param data
        */
        void processTask(MsgStruct data);

        /**
         * 向客户端发送回复
         * @param msg
         */
        void sendReplyToAgent(MessageResponse msg);


        /**
         * 激活当前对象(注意在子类实现时必须要加锁，避免重复激活)
         * @param msgHandlerEngine
         */
        void activeCurrentObject(MessageHandlerEngine msgHandlerEngine,  IMsgHandlerInitializer... initializer);


        /**
         * 释放资源，执行清理操作
         */
        void dispose();
        
        boolean getIsDisposed();
    }

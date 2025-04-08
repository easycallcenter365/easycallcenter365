package com.telerobot.fs.wshandle;

/**
 *  websocket handle 的初始化接口；
 */
public interface IMsgHandlerInitializer {
    void activeCurrentHandlerInstance();
    void destroyHandlerInstance();
}

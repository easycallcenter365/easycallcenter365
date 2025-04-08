package com.telerobot.fs.wshandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InactiveNotice {
    private static final Logger logger = LoggerFactory.getLogger(InactiveNotice.class);

    public  static void onDisconnected(SessionEntity sessionEntity){
              logger.info("收到断开事件, 客户端session信息: {}", sessionEntity.toString() );

    }

}

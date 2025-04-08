package com.telerobot.fs.wshandle;

import com.telerobot.fs.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    protected static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void processArgsError(String msgTips, IMsgHandler handler ,MessageResponse... reply) {
        MessageResponse msg = null;
        if (reply.length != 0) {
            msg = reply[0];
            if (!StringUtils.isNullOrEmpty(msgTips)) {
                msg.setMsg(msgTips);
            }
        } else {
            msg = new MessageResponse();
            msg.setStatus(400);
            msg.setMsg(msgTips);
        }
        handler.sendReplyToAgent(msg);
    }

    public static void processServerInternalError(String msgTips,  IMsgHandler handler , Boolean... displayErrorDetail) {
        MessageResponse msg = new MessageResponse();
        msg.setStatus(500);
        if (displayErrorDetail != null && displayErrorDetail.length != 0) {
            msg.setMsg(msgTips);
        }
        handler.sendReplyToAgent(msg);
        handler.dispose();
        logger.error("{} {}", handler.getTraceId(), msgTips);
    }
}

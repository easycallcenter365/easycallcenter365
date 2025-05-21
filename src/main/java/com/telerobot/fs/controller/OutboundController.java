package com.telerobot.fs.controller;

import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.rmi.runtime.Log;

import javax.servlet.http.HttpServletRequest;

@Controller
@Scope("request")
public class OutboundController {

    private static final Logger logger = LoggerFactory.getLogger(OutboundController.class);

    @RequestMapping("/outboundProcessor")
    @ResponseBody
    public String inboundCall(HttpServletRequest request)  {
        String clientIP = request.getRemoteAddr();
        if (!"127.0.0.1".equalsIgnoreCase(clientIP)) {
            return "Forbidden, only '127.0.0.1' is allowed.";
        }
        // 通过呼叫注册的分机地址，实现外呼
        // 适用于电话交换机网关在内网，而软交换在云端的情况;
        // 此时电话交换机通过一个分机号注册到云端的软交换;
        // 示例： 1002是内网电话交换机注册到云端软交换的一个分机账号：
        //   <extension name="outbound">
        //       <condition field="destination_number"  expression="^(\d{11,13})$" >
        //               <action application="set" data="inherit_codec=true"/>
        //               <action application="set" data="cc_call_leg_uuid=${uuid}" />
        //               <action application="set" data="fullrecordpath=manual/${strftime(%Y/%m/%d/)}${caller_id_number}_$1_${uuid}.wav" />
        //               <action application="record_session" data="$${recording_path}${fullrecordpath}"/>
        //               <action application="log" data="INFO outbound dest number...$1" />
        //               <action application="set" data="contact=${sofia_contact(1002)}" />
        //               <action application="log" data="INFO sofia_contact ... ${contact}" />
        //               <action application="set" data="hangup_after_bridge=true" />
        //               <action application="curl" data="http://127.0.0.1:8870/call-center/outboundProcessor?uuid=${uuid}&amp;user=1002&amp;contact=${url_encode(${contact})}&amp;callee=$1"/>
        //               <action application="park" />
        //      </condition>
        //    </extension>

        String uuid = request.getParameter("uuid");
        String regUser = request.getParameter("user");
        String contact = request.getParameter("contact");
        String callee =  request.getParameter("callee");
        String gateway = contact.replace("sip:" + regUser, callee);
        logger.info("get gateway info {} ", gateway);
        return EslConnectionUtil.sendExecuteCommand("bridge", "{hangup_after_bridge=true}" + gateway, uuid);
    }
}

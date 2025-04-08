package com.telerobot.fs.controller;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.acd.CallHandler;
import com.telerobot.fs.acd.InboundGroupHandler;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.bo.InboundBlack;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.dto.AgentEx;
import com.telerobot.fs.entity.po.CdrDetail;
import com.telerobot.fs.global.CdrPush;
import com.telerobot.fs.robot.RobotChat;
import com.telerobot.fs.service.InboundBlackService;
import com.telerobot.fs.service.InboundDetailService;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadPoolCreator;
import com.telerobot.fs.utils.ThreadUtil;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;


@Controller
@Scope("request")
public class InboundCallController {
	private static final Logger logger = LoggerFactory.getLogger(InboundCallController.class);
	private static int inboundCallThreadPoolSize = Integer.parseInt(
			SystemConfig.getValue("max-call-concurrency", "100")
	);
    private  static ThreadPoolExecutor mainThreadPool = ThreadPoolCreator.create(
			inboundCallThreadPoolSize,
            "inbound-call-thread",
			 365*24,
			inboundCallThreadPoolSize * 2
	);

	@RequestMapping("/inboundProcessor")
	@ResponseBody
	public String inboundCall(HttpServletRequest request) throws InstantiationException, IllegalAccessException {
		final String uuid = request.getParameter("uuid");
		final String caller = request.getParameter("caller").replace("+86", "");
		final String callee = request.getParameter("callee").replace("+86", "");
		final String mediaPort = request.getParameter("local-media-port");
		final String remoteVideoPort = request.getParameter("remote_video_port");
		final String loadTestUuid = request.getParameter("load-test-uuid");
		final String groupId =  request.getParameter("group-id");

		// 在拨号计划中设置录音路径
		String currentThreadPoolInfo = String.format(
				"Current thread pool info：taskCount: %d, activeCount: %d, completedTask: %d, corePoolSize: %d, ",
				mainThreadPool.getTaskCount(),
				mainThreadPool.getActiveCount() - 3,
				mainThreadPool.getCompletedTaskCount(),
				mainThreadPool.getCorePoolSize()
		);
		logger.info("RECV NEW INBOUND CALL, uuid:{}, caller:{}, mediaPort:{}, recordTime: {}, groupId:{}, remoteVideoPort:{}",
				uuid, caller, mediaPort, loadTestUuid, groupId, remoteVideoPort);
		logger.info("uuid: {}, currentThreadPoolInfo: {}", uuid, currentThreadPoolInfo);
		int maxPoolSize =  mainThreadPool.getCorePoolSize();
		if(mainThreadPool.getActiveCount() >=  maxPoolSize){
			logger.error("{} 电话呼入负载过高，请扩容或者调整系统参数!", uuid);
		}

		mainThreadPool.execute(
				new Runnable() {
					@Override
					public void run() {
						logger.info("Processing NEW INBOUND CALL, uuid:{}, caller:{}, recordtime:{}",uuid, caller, mediaPort);
						String mediaFile = genRecordingsFileName(groupId, remoteVideoPort, caller, callee);
						InboundDetail inboundDetail = new InboundDetail(
								UuidGenerator.GetOneUuid(),
								caller,
								callee,
								System.currentTimeMillis(),
								uuid,
								mediaFile,
								groupId,
								remoteVideoPort
						);
						AppContextProvider.getBean(InboundDetailService.class).insertInbound(inboundDetail);
						// 查询黑名单
						InboundBlack inboundBlack = AppContextProvider.getBean(InboundBlackService.class).getInboundBlackByCaller(caller);
                        if(null == inboundBlack) {

                        	boolean transferToPhoneWhileOffline = Boolean.parseBoolean(SystemConfig.getValue("transfer-to-phone-while-agent-offline", "true"));
                        	if(transferToPhoneWhileOffline){
                        		logger.info("{} transfer_to_phone_while_offline enabled. ", inboundDetail.getUuid());
								List<AgentEx> agentList = AppContextProvider.getBean(SysService.class).getAllUserList();
								if(agentList.size() == 0){
									String phone = SystemConfig.getValue("transfer-to-phone-number-while-agent-offline");
									logger.info("{} No users online, try to transfer call to phone {}. ", inboundDetail.getUuid(), phone);
									DoTransferToPhoneWhileOffline(inboundDetail, phone);
									return;
								}
							}

							String recordDir = SystemConfig.getValue("recording_path", "/home/Records/");
							EslConnectionUtil.sendExecuteCommand(
									"record_session",
									recordDir + mediaFile,
									uuid,
									EslConnectionUtil.getDefaultEslConnectionPool()
							);
							logger.info("{} start record_session wav/mp4 {}{}", inboundDetail.getUuid(), recordDir , mediaFile);

							//设置bridge后不挂机;
							EslConnectionUtil.sendExecuteCommand(
									"set",
									"hangup_after_bridge=false",
									uuid,
									EslConnectionUtil.getDefaultEslConnectionPool()
							);

							if(Boolean.parseBoolean(SystemConfig.getValue("ai-answer-call-first","true"))) {
								RobotChat robotChat = new RobotChat(inboundDetail);
								if(!robotChat.getHangup()){
									robotChat.startProcess(uuid);
								}
							}else{
								CallHandler callHandler = new CallHandler(inboundDetail);
								if (InboundGroupHandler.addCallToQueue(callHandler, groupId)) {
									logger.info("{} successfully add call to acd queue.", inboundDetail.getUuid());
								}
							}

						}else{
                        	logger.warn("{} caller {} hit the black list, reject the inbound call.", uuid, caller);
							EslConnectionUtil.sendExecuteCommand(
									"playback",
									"$${sounds_dir}/ivr/hangup.wav",
									uuid,
									EslConnectionUtil.getDefaultEslConnectionPool()
							);
							ThreadUtil.sleep(2000);
							EslConnectionUtil.sendExecuteCommand(
									"hangup",
									"",
									uuid,
									EslConnectionUtil.getDefaultEslConnectionPool()
							);
						}
					}
				});

		return "success";
	}

	public static Map<String, String> parse(String queryString) throws UnsupportedEncodingException {
		Map<String, String> queryPairs = new HashMap<>(16);
		String[] pairs = queryString.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
			String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
			queryPairs.put(key, value);
		}
		return queryPairs;
	}
	private void DoTransferToPhoneWhileOffline(InboundDetail inboundDetail, String destPhone) {
		// gatewayAddr=192.168.67.201:5090&caller=64901409&profile=external&calleePrefix=
		String queryString = SystemConfig.getValue("transfer-to-phone-gw-while-offline");
		Map<String, String> gatewayInfo = null;
		try {
			gatewayInfo = parse(queryString);
		}catch (Throwable throwable){
            logger.error("{} parse  transfer_to_phone_gw_while_offline  params error! ", inboundDetail.getUuid());
		}
        if(gatewayInfo == null){
        	return;
		}

        String outboundUuid = UuidGenerator.GetOneUuid();
		String bridgeString = String.format(
				"{origination_uuid=%s,origination_caller_id_number=%s,effective_caller_id_number=%s}sofia/%s/%s%s@%s ",
				outboundUuid,
				gatewayInfo.get("caller"),
				gatewayInfo.get("caller"),
				gatewayInfo.get("profile"),
				gatewayInfo.get("calleePrefix"),
				destPhone,
				gatewayInfo.get("gatewayAddr")
		);
        //设置bridge后挂机;
		EslConnectionUtil.sendExecuteCommand(
				"set",
				"hangup_after_bridge=true",
				inboundDetail.getUuid(),
				EslConnectionUtil.getDefaultEslConnectionPool()
		);
		inboundDetail.setCallee(destPhone);
		inboundDetail.setExtnum(destPhone);
		inboundDetail.setOpnum(destPhone);

		IEslEventListener listener = new IEslEventListener() {
			private volatile boolean hangup = false;
			@Override
			public synchronized void eventReceived(String addr, EslEvent event) {
				Map<String, String> headers = event.getEventHeaders();
				String uniqueId = headers.get("Unique-ID");
				String eventName = headers.get("Event-Name");
				if (EventNames.CHANNEL_ANSWER.equalsIgnoreCase(eventName)) {
					inboundDetail.setAnsweredTime(System.currentTimeMillis());
				} else if (EventNames.CHANNEL_HANGUP.equalsIgnoreCase(eventName)) {
					if(!hangup){
						hangup = true;
						// 推送话单;
						if(inboundDetail.getAnsweredTime() > 0L) {
							inboundDetail.setAnsweredTimeLen(System.currentTimeMillis() - inboundDetail.getAnsweredTime());
						}
						inboundDetail.setHangupTime(System.currentTimeMillis());
						CdrDetail cdrDetail = new CdrDetail();
						cdrDetail.setUuid(inboundDetail.getUuid());
						cdrDetail.setCdrType("inbound");
						cdrDetail.setCdrBody(JSON.toJSONString(inboundDetail));
						CdrPush.addCdrToQueue(cdrDetail);
						logger.info("{} 话单已推送", inboundDetail.getUuid());
					}
					String uuidKill = "";
					if(uniqueId.equals(outboundUuid)){
						uuidKill = inboundDetail.getUuid();
					}else{
						uuidKill = outboundUuid;
					}
					EslConnectionUtil.sendAsyncApiCommand("uuid_kill", uuidKill);
				}
			}
			@Override
			public void backgroundJobResultReceived(String addr, EslEvent event) {

			}
		};

		EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().addListener(outboundUuid, listener);
		EslConnectionUtil.getDefaultEslConnectionPool().getDefaultEslConn().addListener(inboundDetail.getUuid(), listener);
		EslConnectionUtil.sendExecuteCommand(
				"bridge",
				bridgeString,
				inboundDetail.getUuid()
		);
	}

	private String genRecordingsFileName(String groupId, String remoteVideoPort, String caller, String callee){
		String recordFileExtension = SystemConfig.getValue("recordings_extension", "wav");
		if(!StringUtils.isNullOrEmpty(remoteVideoPort)){
			recordFileExtension = "mp4";
		}
		String dateStr = DateUtils.format(new Date(), "yyyy/MM/dd/HH");
		String fileName = caller + "_" + callee + "_" + DateUtils.format(new Date(), "mmss");
        return String.format("%s/%s/%s.%s", groupId,  dateStr, fileName,  recordFileExtension);
	}

}

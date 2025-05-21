package com.telerobot.fs.controller;

import com.telerobot.fs.acd.CallHandler;
import com.telerobot.fs.acd.InboundGroupHandler;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.bo.InboundBlack;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.robot.RobotChat;
import com.telerobot.fs.service.InboundBlackService;
import com.telerobot.fs.service.InboundDetailService;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.utils.ThreadPoolCreator;
import com.telerobot.fs.utils.ThreadUtil;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;


// public.xml config sample:
/**
 *
 *    <extension name="inbound_acd">
 *           <condition field="destination_number"  expression="^(99999easycallcenter365)$" >
 *               <action application="set" data="inherit_codec=true"/>
 *               <action application="set" data="not_save_record_flag=1"/>
 *               <action application="answer" />
 *               <action application="start_dtmf"/>
 *               <action application="export" data="RECORD_STEREO=true"/>
 *               <action application="set" data="RECORD_STEREO=true"/>
 *               <action application="log" data="INFO ${uuid}  caller=${caller_id_number}, callee=$1 " />
 *               <action application="log" data="INFO local_media_port=${local_media_port}, remote_media_port=${remote_media_port}, local_video_port=${local_video_port}, remote_video_port=${remote_video_port}" />
 *               <action application="set" data="record_sample_rate=8000"/>
 *               <action application="set" data="RECORD_STEREO=false"/>
 *               <action application="set" data="continue_on_fail=true"/>
 *               <action application="set" data="hangup_after_bridge=false"/>
 *               <action application="set" data="record-time=${strftime(%Y%m%d%H%M%S)}" />
 *               <action application="set" data="groupId=1"/>
 *               <action application="set" data="send_silence_when_idle=-1"/>
 *               <action application="curl" data="http://127.0.0.1:8870/call-center/inboundProcessor?remote_video_port=${remote_video_port}&amp;local-media-port=${local_media_port}&amp;uuid=${uuid}&amp;caller=${caller_id_number}&amp;callee=$1&amp;load-test-uuid=${uuid}&amp;wav-file=${wav-file}&amp;group-id=${groupId}"/>
 *               <action application="park" />
 *           </condition>
 *       </extension>
 *
 */

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
		String clientIP = request.getRemoteAddr();
		if(!"127.0.0.1".equalsIgnoreCase(clientIP)){
			return  "Forbidden, only '127.0.0.1' is allowed.";
		}

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

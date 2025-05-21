package com.telerobot.fs.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.entity.po.CdrDetail;
import com.telerobot.fs.entity.po.CdrEntity;
import com.telerobot.fs.entity.po.CdrEntityEx;
import com.telerobot.fs.global.CdrPush;
import com.telerobot.fs.service.CdrService;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.RequestUtils;
import com.telerobot.fs.utils.StringUtils;
import io.netty.util.internal.StringUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by zl on 2015/8/27.
 *  优化后，增加了ValidTimeLenMills字段，记录通话的时长，精确到毫秒；
 *  产生的话单数据可用于计费使用;
 *  注意2点事项：
 *    1. 需要记录话单b腿，即被叫的话单数据，需要开启Freeswitch话单模块 xml_cdr 中推送B腿的配置:
 *     <param name="log-b-leg" value="true"/>
 *   2. 话单中默认记录的是录音的完整全路径，录音文件名中附带客户手机号码信息，为了避免客户信息泄露，
 *    在调听录音时，需要通过java读取路径文件流返回给客户端，而不是返回带文件名的全路径。
 *
 */
@Controller
@RequestMapping("/cdr-ex")
public class CdrControllerEx {

	private static Logger logger =  LoggerFactory.getLogger(CdrControllerEx.class);

	@Autowired
	private CdrService service;


	@RequestMapping("/record")
	@ResponseBody
	public String postCalling(HttpServletRequest request,Map<String,Object> model) throws Exception {
		Map<String, Object> params = RequestUtils.parameterValuesToMap(request);
		if(!params.containsKey("cdr")) {
			return "No cdr found.";
		}
		String cdr = (String)params.get("cdr");
		Map<String,String> varItems = analysis(cdr);

		String direction = varItems.get("direction"); //外呼方向 outbound、inbound
		String auto_batchcall_flag = varItems.get("auto_batchcall_flag"); //预测外呼话单标志
		String not_save_record_flag = varItems.get("not_save_record_flag");//不保存话单标志
		String callspy_flag = varItems.get("callspy_flag");//通话监听话单标志
		String start_stamp = varItems.get("start_stamp"); // 开始时间
		String answer_stamp = varItems.get("answer_stamp"); // 应答时间
		String end_stamp = varItems.get("end_stamp"); // 结束时间
		String caller_id_number = varItems.get("caller_id_number"); // 主叫号码
		String destination_number = varItems.get("destination_number");// 被叫号码
		String uuid = varItems.get("uuid"); //通话唯一id
		String hangup_cause = varItems.get("last_bridge_hangup_cause"); //挂机原因
		String sip_auth_username = varItems.get("sip_auth_username"); //分机号
		String projectId = varItems.get("projectid"); //所属项目编号; 使用 profile_name
		String extNum = varItems.get("extnum");
		String opNum = varItems.get("opnum");
		String destPhoneNum = varItems.get("destphonenum");
		String fullRecordPath = varItems.get("fullrecordpath");
		String ccCallLegUuid = varItems.get("cc_call_leg_uuid");
		long answerUepoch = Long.parseLong(varItems.get("answer_uepoch"));
		long endUepoch = Long.parseLong(varItems.get("end_uepoch"));
		String transferToConferenceTime = varItems.get("transfer_to_conference_time");

		String tips = "";
		if("1".equals(not_save_record_flag)) {
			tips = "not_save_record_flag detected.";
			return tips;
		}
		if("1".equals(callspy_flag)) {
			tips = "callspy_flag detected.";
			return tips;
		}
		if("1".equalsIgnoreCase(auto_batchcall_flag)){
			tips = "auto_batchcall_flag detected.";
			return tips;
		}
		if(StringUtils.isNullOrEmpty(fullRecordPath)){
			tips = "fullRecordPath is null ,skip this recordings.";
			logger.info(tips);
			return tips;
		}

		CdrEntityEx cdrEntity = new CdrEntityEx();
		cdrEntity.setProjectId(projectId);
		cdrEntity.setEnd_time(DateUtils.parseDateTime(URLDecoder.decode(end_stamp,"utf-8")));
		if(!StringUtils.isNullOrEmpty(transferToConferenceTime)){
			cdrEntity.setEnd_time(DateUtils.parseDateTime(URLDecoder.decode(transferToConferenceTime,"utf-8")));
		}
		cdrEntity.setStart_time(DateUtils.parseDateTime(URLDecoder.decode(start_stamp,"utf-8")));
		if(!StringUtils.isNullOrEmpty(answer_stamp)){
			cdrEntity.setAnswer_time(DateUtils.parseDateTime(URLDecoder.decode(answer_stamp,"utf-8")));
			cdrEntity.setValidTimeLen(DateUtils.secondsBetween(cdrEntity.getAnswer_time(), cdrEntity.getEnd_time()));
			long timeLenMills = (endUepoch - answerUepoch)/1000;
			cdrEntity.setValidTimeLenMills((int)timeLenMills);
		}
		cdrEntity.setCallee(destination_number);
		if(!StringUtils.isNullOrEmpty(destPhoneNum)){
			cdrEntity.setCallee(destPhoneNum);
		}
		cdrEntity.setCaller(caller_id_number);
		if(!StringUtils.isNullOrEmpty(extNum)){
			cdrEntity.setCaller(extNum);
		}
        if(ccCallLegUuid.length() > 0){
        	uuid = ccCallLegUuid;
		}
		if(fullRecordPath.toLowerCase().endsWith(".mp4")){
			cdrEntity.setCallType("video");
		}else{
			cdrEntity.setCallType("audio");
		}
		cdrEntity.setUuid(uuid);
		cdrEntity.setTimeLen(DateUtils.secondsBetween(cdrEntity.getStart_time(), cdrEntity.getEnd_time()));
		cdrEntity.setExtNum(sip_auth_username);
		if(!StringUtils.isNullOrEmpty(extNum)){
			cdrEntity.setExtNum(extNum);
		}
		if(!StringUtils.isNullOrEmpty(opNum)){
			cdrEntity.setOpNum(opNum);
		}
		if(StringUtils.isNullOrEmpty(hangup_cause)) {
			hangup_cause = "NORMAL_CLEARING";
		}
		cdrEntity.setHangup_cause(hangup_cause);
		cdrEntity.setFullRecordPath(fullRecordPath);

		if(service.saveCdr(cdrEntity))
		{
			CdrDetail cdrDetail = new CdrDetail();
			cdrDetail.setUuid(uuid);
			cdrDetail.setCdrType("outbound");
			cdrDetail.setCdrBody(JSON.toJSONString(cdrEntity));
			CdrPush.addCdrToQueue(cdrDetail);
		}
		return "success";
	}


	
	/***
	 * 解析xml话单
	 ***/
	private static Map<String,String> analysis(String fileContent) throws Exception {
		Map<String, String> itemMaps = new HashMap<String, String>(500);
		SAXReader reader = new SAXReader();
		InputStream input = new ByteArrayInputStream(fileContent.getBytes());
		Document document = reader.read(input);
		Element root = document.getRootElement();
		List<Element> childElements = root.elements();
		for (Element child : childElements) {
			if ("variables".equalsIgnoreCase(child.getName())) {
				List<Element> varElements = child.elements();
				for (Element var : varElements) {
					String elementName = var.getName().toLowerCase().trim();
					itemMaps.put(elementName, var.getTextTrim());
				}
			}

			if ("callflow".equals(child.getName())) {
				List<Element> varElements = child.elements();
				for (Element varEle : varElements) {
					if ("caller_profile".equals(varEle.getName())) {
						List<Element> varElementsSub = varEle.elements();
						for (Element var : varElementsSub) {
							String elementName = var.getName().toLowerCase().trim();
							switch (elementName) {
								case "caller_id_number":
								case "destination_number":
									itemMaps.put(elementName, var.getTextTrim());
									break;
								default:
									break;
							}
						}
					}
				}
			}
		}
       return itemMaps;
	}
}

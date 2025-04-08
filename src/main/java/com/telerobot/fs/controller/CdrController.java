package com.telerobot.fs.controller;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.po.CdrEntity;
import com.telerobot.fs.service.CdrService;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.RequestUtils;
import com.telerobot.fs.utils.StringUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
/**
 *  在Freeswitch的配置文件  xml_cdr.conf.xml 中配置当前接收话单的api地址;
 *  通过话单模块推送有个好处，即使工具条断开，后端仍然可以收到话单消息，一定程度保证消息不会丢失;
 */
@Controller
@RequestMapping("/cdr")
@Scope("request")
public class CdrController {
	private final static Logger logger = LoggerFactory.getLogger(CdrController.class);

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
		analysis(cdr);
		if(not_save_record_flag.equals("1")) {
			return "not_save_record_flag detected, skipping saving cdr.";
		}
		if(callspy_flag.equals("1")) {
			return "callspy_flag detected, skipping saving cdr.";
		}

		if(auto_batchcall_flag.equals("1")){
			return "auto_batchcall_flag detected, skipping saving cdr.";
		}

		CdrEntity cdrEntity = new CdrEntity();
		cdrEntity.setProjectId(projectId);
		cdrEntity.setCaseNo(caseno);
		cdrEntity.setEndTime(DateUtils.parseDateTime(URLDecoder.decode(end_stamp,"utf-8")));
		cdrEntity.setStartTime(DateUtils.parseDateTime(URLDecoder.decode(start_stamp,"utf-8")));

		if(!StringUtils.isNullOrEmpty(my_answer_stamp)){
			cdrEntity.setAnswerTime(DateUtils.parseDateTime(URLDecoder.decode(my_answer_stamp,"utf-8")));
			cdrEntity.setValidTimeLen(DateUtils.secondsBetween(cdrEntity.getAnswerTime(), cdrEntity.getEndTime()));
		}
		cdrEntity.setCallee(destination_number);
		if(!StringUtils.isNullOrEmpty(callee)){
			cdrEntity.setCallee(callee);
		}
		cdrEntity.setCaller(caller_id_number);
		if(!StringUtils.isNullOrEmpty(caller)){
			cdrEntity.setCaller(caller);
		}
		cdrEntity.setUuid(uuid);
		cdrEntity.setTimeLen(DateUtils.secondsBetween(cdrEntity.getStartTime(), cdrEntity.getEndTime()));
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
		cdrEntity.setHangupCause(hangup_cause);

		if(hangup_cause.equalsIgnoreCase("manually_hangup")) {
			cdrEntity.setCustomerFirstHangup(1);
		}else{
			cdrEntity.setCustomerFirstHangup(0);
		}
		cdrEntity.setId(UuidGenerator.GetOneUuid());
        //催收系统不需要完整的录音文件名称，只需要文件名
		cdrEntity.setFullRecordPath(fullRecordPath);


		// 设置发送post请求到催收系统，发送话单;
		//使用OKHttpClient发送请求，如果失败则记录状态；把任务投递到线程池中;
		String json = JSON.toJSONString(cdrEntity);
		if(StringUtils.isNullOrEmpty(caseno)){
           logger.info("收到话单保存请求:{}，由于caseno为空，不再向催收系统发送保存请求.", json);
		}
	 	boolean postSuccess = false;
		String collRecordCdrUrl = SystemConfig.getValue("coll_record_cdr_url");
        postSuccess = service.postCdrToColl(cdrEntity);
        logger.info("收到话单保存请求:{}，发送到催收系统是否成功: {}, {}", json, postSuccess, collRecordCdrUrl );

 		if(StringUtils.isNullOrEmpty(caseno)){
			cdrEntity.setSavedCdr(1);
			//案件号为空的情况下，设置savedCdr标志为1，避免定时任务提交该话单到催收系统;
		}

 		//话单表cdr，需要存储完整路径名称; 这里把完整路径拼接上;
		if(!StringUtils.isNullOrEmpty(fullRecordPath)){
			cdrEntity.setFullRecordPath(fullRecordPath);
		}
		if(!service.saveCdr_Original(cdrEntity))
		{
			logger.info("话单保存到数据库失败:{}，发送到催收系统是否成功: {}", json, postSuccess );
			throw new RuntimeException("话单保存失败.");
			// let Freeswitch server log failed cdrs via server 500 error.
		}
		return "success";
		//必须返回success，便于shell脚本处理未保存的话单;
	}
	
	private String auto_batchcall_flag = ""; //预测外呼话单标志
	private String not_save_record_flag = "";//不保存话单标志
	private String callspy_flag = "";//通话监听话单标志
	private String start_stamp = ""; // 开始时间
	private String answer_stamp = ""; // 应答时间
	private String end_stamp = ""; // 结束时间
	private String caller_id_number = ""; // 主叫号码
	private String destination_number = "";// 被叫号码
	private String uuid = ""; //通话唯一id
	private String hangup_cause = ""; //挂机原因
	private String sip_auth_username = ""; //分机号
	private String projectId = ""; //所属项目编号; 使用 profile_name
	private String my_answer_stamp = ""; //外线接通时间;
	private String extNum;
	private String opNum;
	private String callee;
	private String caller;
	private String caseno;
	private String fullRecordPath = "";
	
	/***
	 * 解析xml话单
	 ***/
	public void analysis(String fileContent) throws Exception {
		SAXReader reader = new SAXReader();
		InputStream input = new ByteArrayInputStream(fileContent.getBytes());
		Document document = reader.read(input);
		Element root = document.getRootElement();
		List<Element> childElements = root.elements();
		for (Element child : childElements) {
			if (child.getName().equalsIgnoreCase("variables")) {
				List<Element> varElements = child.elements();
				for (Element var : varElements) {
					String elementName = var.getName().toLowerCase().trim();
					switch (elementName) {
					case "uuid":
						uuid = var.getTextTrim();
						break;
					case "sip_auth_username":
						sip_auth_username = var.getTextTrim();
						break;
					case "start_stamp":
						start_stamp = var.getTextTrim();
						break;
					case "answer_stamp":
						answer_stamp = var.getTextTrim();
						break;
					case "end_stamp":
						end_stamp = var.getTextTrim();
						break;
					case "auto_batchcall_flag":
						auto_batchcall_flag = var.getTextTrim();
						break;
					case "not_save_record_flag":
						not_save_record_flag = var.getTextTrim();
						break;
					case "callspy_flag":
						callspy_flag = var.getTextTrim();
						break;
					case "last_bridge_hangup_cause":
						hangup_cause += var.getTextTrim();
						break;
					 case "projectid":
							projectId = var.getTextTrim();
							break;
					case "extnum":
						extNum = var.getTextTrim();
						break;
					case "opnum":
						opNum = var.getTextTrim();
						break;
					case "callee":
						callee = var.getTextTrim();
						break;
				    case "caller":
						caller = var.getTextTrim();
						break;
					case "caseno":
						caseno = var.getTextTrim();
						break;
				    case "my_answer_stamp":
					    	my_answer_stamp = var.getTextTrim();
							break;
				    case "fullrecordpath":
							fullRecordPath = var.getTextTrim();
							break;
					}
				}
			}

			if (child.getName().equalsIgnoreCase("callflow")) {
				List<Element> varElements = child.elements();
				for (Element varEle : varElements) {
					if (varEle.getName().equalsIgnoreCase("caller_profile")) {
						List<Element> varElementsSub = varEle.elements();
						for (Element var : varElementsSub) {
							String elementName = var.getName().toLowerCase().trim();
							switch (elementName) {
							case "caller_id_number":
								caller_id_number = var.getTextTrim();
								break;
							case "destination_number":
								destination_number = var.getTextTrim();
								break;
							}
						}
					}
				}
			}
		}
	}
}

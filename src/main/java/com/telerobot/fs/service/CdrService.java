package com.telerobot.fs.service;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.po.CdrEntity;
import com.telerobot.fs.entity.po.CdrEntityEx;
import com.telerobot.fs.utils.*;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.po.CdrEntity;
import com.telerobot.fs.entity.po.CdrEntityEx;
import com.telerobot.fs.utils.DateUtils;
import com.telerobot.fs.utils.OkHttpClientUtil;
import com.telerobot.fs.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Service
public class CdrService {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Resource
	private JdbcTemplate jdbcTemplate;

	public boolean saveCdr(CdrEntityEx cdr)
	{
		try {
			String execSql = "INSERT INTO  `cc_outbound_cdr` (`id`, `caller`, `opnum`, `callee`, `start_time`, `answered_time`, `end_time`," +
					" `uuid`, `call_type`, `time_len`, `time_len_valid`, `record_filename`, `hangup_cause`)  "
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			int affectRow = jdbcTemplate.update(execSql,
					new Object[]{
							UuidGenerator.GetOneUuid(),
							cdr.getCaller(),
							cdr.getOpNum(),
							cdr.getCallee(),
							cdr.getStart_time().getTime(),
							cdr.getAnswer_time().getTime(),
							cdr.getEnd_time().getTime(),
							cdr.getUuid(),
							cdr.getCallType(),
							cdr.getTimeLen(),
							cdr.getValidTimeLenMills(),
							cdr.getFullRecordPath(),
							cdr.getHangup_cause()
					});
			return affectRow == 1;
		}catch (Throwable e){
			log.error("{} save cdr to database error: {}  {} {}",
					cdr.getUuid(),
					JSON.toJSONString(cdr),
					e.toString(),
					CommonUtils.getStackTraceString(e.getStackTrace())
			);
			return false;
		}
	}


	public boolean saveCdr_Original(CdrEntity cdr) {
		String execSql = "INSERT INTO  `cdr` (`id`, `record_type`, `uuid`, `opNum` , `extNum`, `caller`, `callee`, `start_time`, `end_time`, `answer_time`, `hangup_cause`, `TimeLen`, `ValidTimeLen`, `projectId`, `caseNo`, `customerFirstHangup`, `full_record_filename`,`savedCdr`)  "
				+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  ?, ?)";
		int affectRow = jdbcTemplate.update(execSql,
				new Object[] {
						cdr.getId(),
						cdr.getRecord_type(),
						cdr.getUuid() ,
						cdr.getOpNum(),
						cdr.getExtNum(),
						cdr.getCaller(),
						cdr.getCallee(),
						cdr.getStartTime(),
						cdr.getEndTime(),
						cdr.getAnswerTime(),
						cdr.getHangupCause(),
						cdr.getTimeLen(),
						cdr.getValidTimeLen(),
						cdr.getProjectId(),
						cdr.getCaseNo(),
						cdr.getCustomerFirstHangup(),
						cdr.getFullRecordPath(),
						cdr.getSavedCdr()
				});
		return affectRow == 1;
	}

	/**
	 * 发送cdr话单信息到催收系统;
	 * @return
	 */
	public boolean postCdrToColl(CdrEntity cdr){
		if(StringUtils.isNullOrEmpty(cdr.getCaseNo())){
			return false;
		}
		String coll_record_cdr_url =  SystemConfig.getValue("coll_record_cdr_url");
		if(StringUtils.isNullOrEmpty(coll_record_cdr_url)){
			throw new RuntimeException("没有配置催收系统接收话单的参数 coll_record_cdr_url.");
		}
		String json = "{" +
				"    \"id\":\""+ cdr.getId() +"\"," +
				"    \"uuid\":\""+ cdr.getUuid() +"\"," +
				"    \"recordType\":\".wav\"," +
				"    \"caller\":\""+ cdr.getCaller() +"\"," +
				"    \"callee\":\""+ cdr.getCallee() +"\"," +
				"    \"startTime\":\""+ DateUtils.formatDateTime(cdr.getStartTime()) +"\"," +
				"    \"opNum\":\""+ cdr.getOpNum() +"\"," +
				"    \"endTime\":\""+ DateUtils.formatDateTime(cdr.getEndTime()) +"\"," +
				"    \"answerTime\":\""+ DateUtils.formatDateTime(cdr.getAnswerTime())  +"\"," +
				"    \"hangupCause\":\""+ cdr.getHangupCause() +"\"," +
				"    \"customerFirstHangup\":\""+ cdr.getCustomerFirstHangup() +"\"," +
				"    \"validTimeLen\":\""+ cdr.getValidTimeLen() +"\"," +
				"    \"timeLen\":\""+ cdr.getTimeLen() +"\"," +
				"    \"full_record_filename\":\""+ cdr.getFullRecordPath() +"\"," +
				"    \"caseNo\":\""+ cdr.getCaseNo() +"\"" +
				"}";
		boolean success = false;
		String response = OkHttpClientUtil.curl(coll_record_cdr_url, json, "post") ;
		log.info(String.format("request url %s , response: %s", coll_record_cdr_url, response));
		if(!StringUtils.isNullOrEmpty(response)){
			JSONObject jsonObject =   JSON.parseObject(response);
			if("200".equals(jsonObject.getString("code"))){
				cdr.setSavedCdr(1);
				success = true;
			}else{
				cdr.setSavedCdr(0);
			}
		}
		return success;
	}


	public boolean updateCdr(CdrEntity cdr) {
		String execSql = "update  `cdr`  set  savedCdr=? where id=?";
		int affectRow = jdbcTemplate.update(execSql,
				new Object[] {
						cdr.getSavedCdr(),
						cdr.getId()
				});
		return affectRow == 1;
	}

	/**
	 *  获取未保存处理的话单列表;
	 * @return
	 */
	public List<CdrEntity> getUnSavedCdrList() {
		List<CdrEntity> cdrList = new LinkedList<CdrEntity>();
		String checkSQL = "select * from  cdr  where savedCdr=0 ";
		RowMapper<CdrEntity> rowMapper = new BeanPropertyRowMapper<CdrEntity>(CdrEntity.class);
		try {
			cdrList = this.jdbcTemplate.query(checkSQL, rowMapper);
		} catch (Exception e) {
			log.error("获取话单列表时发生错误！" + e.toString());
		}
		return cdrList;
	}

	public   void processFailedCdr(){
		log.info("启动话单定时重传任务...");
		List<CdrEntity> cdrList = getUnSavedCdrList();

		//催收系统不需要完整的录音文件名称，只需要文件名; 这里统一处理下所有的路径;
		for (CdrEntity cdrEntity : cdrList) {
			String recordPath = cdrEntity.getFullRecordPath();
			if(!StringUtils.isNullOrEmpty(recordPath)){
				cdrEntity.setFullRecordPath(new File(recordPath).getName());
			}
		}

		for (int i = 0; i < cdrList.size(); i++) {
			CdrEntity cdrEntity  = cdrList.get(i);
			boolean postSuccess =  postCdrToColl(cdrEntity);
			if(postSuccess) {
				if(!updateCdr(cdrEntity)) {
					log.info("定时任务，话单保存失败：" + JSON.toJSONString(cdrEntity));
				}
			}
		}
	}



}

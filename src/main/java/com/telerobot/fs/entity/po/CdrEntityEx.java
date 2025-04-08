package com.telerobot.fs.entity.po;

import java.util.Date;

import com.telerobot.fs.utils.DateUtils;

/***
    * cdr 实体类
    ***/ 
public class CdrEntityEx {

	   public CdrEntityEx() {
	   }

	   private String record_type = ".wav";
	   private String uuid = "";
	   private String extNum = "";
	   private String opNum = "";
	   private String caller = "";
	   private String callee = "";
	   private Date start_time;
	   private Date end_time;
	   private Date answer_time = DateUtils.parseDateTime("1980-1-1 0:00:00");
	   private String hangup_cause = "";
	   private int timeLen;
	   private int validTimeLen = 0;
	   private String projectId = "";
	   private String fullRecordPath;
	   private int ValidTimeLenMills;
	   private String callType;

	   /**
		* 录音类型
		**/
	   public void setRecord_type(String record_type) {
		   this.record_type = record_type;
	   }

	   /**
		* 录音类型
		**/
	   public String getRecord_type() {
		   return record_type;
	   }

	   /**
		* 通话唯一编号,用作录音文件名称
		**/
	   public void setUuid(String uuid) {
		   this.uuid = uuid;
	   }

	   /**
		* 通话唯一编号,用作录音文件名称
		**/
	   public String getUuid() {
		   return uuid;
	   }

	   /**
		* 分机号码
		**/
	   public void setExtNum(String extNum) {
		   this.extNum = extNum;
	   }

	   /**
		* 分机号码
		**/
	   public String getExtNum() {
		   return extNum;
	   }

		public String getCallType() {
			return callType;
		}

		public void setCallType(String callType) {
			this.callType = callType;
		}

	/**
		* 工号
		**/
	   public void setOpNum(String opNum) {
		   this.opNum = opNum;
	   }

	   /**
		* 工号
		**/
	   public String getOpNum() {
		   return opNum;
	   }

	   /**
		* 通话的主叫号码
		**/
	   public void setCaller(String caller) {
		   this.caller = caller;
	   }

	   /**
		* 通话的主叫号码
		**/
	   public String getCaller() {
		   return caller;
	   }

	   /**
		* 通话的被叫号码
		**/
	   public void setCallee(String callee) {
		   this.callee = callee;
	   }

	   /**
		* 通话的被叫号码
		**/
	   public String getCallee() {
		   return callee;
	   }

	   /**
		* 通话开始时间
		**/
	   public void setStart_time(Date start_time) {
		   this.start_time = start_time;
	   }

	   /**
		* 通话开始时间
		**/
	   public Date getStart_time() {
		   return start_time;
	   }

	   /**
		* 通话结束时间
		**/
	   public void setEnd_time(Date end_time) {
		   this.end_time = end_time;
	   }

	   /**
		* 通话结束时间
		**/
	   public Date getEnd_time() {
		   return end_time;
	   }

	   /**
		* 通话应答时间
		**/
	   public void setAnswer_time(Date answer_time) {
		   this.answer_time = answer_time;
	   }

	   /**
		* 通话应答时间
		**/
	   public Date getAnswer_time() {
		   return answer_time;
	   }

	   /**
		* 挂机原因
		**/
	   public void setHangup_cause(String hangup_cause) {
		   this.hangup_cause = hangup_cause;
	   }

	   /**
		* 挂机原因
		**/
	   public String getHangup_cause() {
		   return hangup_cause;
	   }

	   /**
		* 通话时长,秒
		**/
	   public void setTimeLen(int TimeLen) {
		   this.timeLen = TimeLen;
	   }

	   /**
		* 通话时长,秒
		**/
	   public int getTimeLen() {
		   return timeLen;
	   }

	   /**
		* @return 有效通话时长, 秒
		*/
	   public int getValidTimeLen() {
		   return validTimeLen;
	   }

	   public String getProjectId() {
		   return projectId;
	   }

	   public void setProjectId(String projectId) {
		   this.projectId = projectId;
	   }

	   public void setValidTimeLen(int validTimeLen) {
		   this.validTimeLen = validTimeLen;
	   }

	   public String getFullRecordPath() {
		   return fullRecordPath;
	   }

	   public void setFullRecordPath(String fullRecordPath) {
		   this.fullRecordPath = fullRecordPath;
	   }

	   public int getValidTimeLenMills() {
		   return ValidTimeLenMills;
	   }

	   public void setValidTimeLenMills(int validTimeLenMills) {
		   ValidTimeLenMills = validTimeLenMills;
	   }
   }


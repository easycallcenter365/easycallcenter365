package com.telerobot.fs.entity.po;

import java.util.Date;

import com.telerobot.fs.utils.DateUtils;

/***
    * cdr 实体类
    ***/ 
public class CdrEntity {

	public  CdrEntity()  {}

	private String id = "";
	private String record_type =".wav";
	private String uuid = "";
	private String extNum = "";
	private String opNum = "";
	private String caller = "";
	private String callee = "";
	private Date startTime;
	private Date endTime;
	private Date answerTime = DateUtils.parseDateTime("1980-1-1 0:00:00");
	private String hangupCause = "";
	private int timeLen;
	private int validTimeLen=0;
	private String projectId = "";
	private String caseNo="";
	private int savedCdr = 0;
	private int customerFirstHangup = 1;
	private String fullRecordPath = "";

	   public String getId() {
		   return id;
	   }

	   public void setId(String id) {
		   this.id = id;
	   }

	   /**  录音类型  **/
	public void setRecord_type(String record_type){
		this.record_type = record_type;
	}

	/**  录音类型  **/
	public String getRecord_type(){
		return record_type;
	}

	/**  通话唯一编号,用作录音文件名称  **/
	public void setUuid(String uuid){
		this.uuid=uuid;
	}

	/**  通话唯一编号,用作录音文件名称  **/
	public String getUuid(){
		return uuid;
	}

	/**  分机号码  **/
	public void setExtNum(String extNum){
		this.extNum=extNum;
	}

	/**  分机号码  **/
	public String getExtNum(){
		return extNum;
	}
	
	/**  工号   **/
	public void setOpNum(String opNum){
		this.opNum = opNum;
	}

	/**  工号  **/
	public String getOpNum(){
		return opNum;
	}

	/**  通话的主叫号码  **/
	public void setCaller(String caller){
		this.caller=caller;
	}

	/**  通话的主叫号码  **/
	public String getCaller(){
		return caller;
	}

	/**  通话的被叫号码  **/
	public void setCallee(String callee){
		this.callee=callee;
	}

	/**  通话的被叫号码  **/
	public String getCallee(){
		return callee;
	}

	/**  通话开始时间  **/
	public void setStartTime(Date startTime){
		this.startTime = startTime;
	}

	/**  通话开始时间  **/
	public Date getStartTime(){
		return startTime;
	}

	/**  通话结束时间  **/
	public void setEndTime(Date endTime){
		this.endTime = endTime;
	}

	/**  通话结束时间  **/
	public Date getEndTime(){
		return endTime;
	}

	/**  通话应答时间  **/
	public void setAnswerTime(Date answerTime){
		this.answerTime = answerTime;
	}

	/**  通话应答时间  **/
	public Date getAnswerTime(){
		return answerTime;
	}

	/**  挂机原因  **/
	public void setHangupCause(String hangupCause){
		this.hangupCause = hangupCause;
	}

	/**  挂机原因  **/
	public String getHangupCause(){
		return hangupCause;
	}

	/**  通话时长,秒  **/
	public void setTimeLen(int TimeLen){
		this.timeLen=TimeLen;
	}

	/**  通话时长,秒  **/
	public int getTimeLen(){
		return timeLen;
	}

	/**
	 * @return 有效通话时长,秒
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

	   public String getCaseNo() {
		   return caseNo;
	   }

	   public void setCaseNo(String caseNo) {
		   this.caseNo = caseNo;
	   }

	   public int getSavedCdr() {
		   return savedCdr;
	   }

	   public void setSavedCdr(int savedCdr) {
		   this.savedCdr = savedCdr;
	   }

	   public int getCustomerFirstHangup() {
		   return customerFirstHangup;
	   }

	   public void setCustomerFirstHangup(int customerFirstHangup) {
		   this.customerFirstHangup = customerFirstHangup;
	   }

	   public String getFullRecordPath() {
		   return fullRecordPath;
	   }

	   public void setFullRecordPath(String fullRecordPath) {
		   this.fullRecordPath = fullRecordPath;
	   }
   }


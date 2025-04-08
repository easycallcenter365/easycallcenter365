package com.telerobot.fs.entity.bo;
import com.telerobot.fs.utils.DateUtils;

import java.util.Date;

   /***
    * Acd 实体类
    ***/ 
public class AcdEntity {

	public  AcdEntity()  {}
 
	public AcdEntity(String phone_number, String uniqueid, int groud_id, String opnum, String extnum, short is_answered,
			Date answered_time, Date call_create_time, Date hangup_time, int time_len) {
		this.phone_number = phone_number;
		this.uniqueid = uniqueid;
		this.groud_id = groud_id;
		this.opnum = opnum;
		this.extnum = extnum;
		this.is_answered = is_answered;
		this.answered_time = answered_time;
		this.call_create_time = call_create_time;
		this.hangup_time = hangup_time;
		this.time_len = time_len;
	}
 
	private String phone_number = "";
	private String uniqueid = "";
	private int groud_id = 0;
	private String opnum = "";
	private String extnum = "";
	private int is_answered = 0;
	private Date answered_time = DateUtils.parseDateTime("1980-01-01 00:00:00");
	private Date call_create_time = DateUtils.parseDateTime("1980-01-01 00:00:00");
	private Date hangup_time = DateUtils.parseDateTime("1980-01-01 00:00:00");
	private int time_len = 0;
	private int Id = 0;
	

	/**
	 * @return the id
	 */
	public int getId() {
		return Id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		Id = id;
	}

	/**  呼入的客户手机或者固话号码;  **/
	public void setPhone_number(String phone_number){
		this.phone_number=phone_number;
	}

	/**  呼入的客户手机或者固话号码;  **/
	public String getPhone_number(){
		return phone_number;
	}

	/**  通话的唯一标识  **/
	public void setUniqueid(String uniqueid){
		this.uniqueid=uniqueid;
	}

	/**  通话的唯一标识  **/
	public String getUniqueid(){
		return uniqueid;
	}

	/**  请求服务的业务组  **/
	public void setGroud_id(int groud_id){
		this.groud_id=groud_id;
	}

	/**  请求服务的业务组  **/
	public int getGroud_id(){
		return groud_id;
	}

	/**  员工工号  **/
	public void setOpnum(String opnum){
		this.opnum=opnum;
	}

	/**  员工工号  **/
	public String getOpnum(){
		return opnum;
	}

	/**  应答该通电话的分机号  **/
	public void setExtnum(String extnum){
		this.extnum=extnum;
	}

	/**  应答该通电话的分机号  **/
	public String getExtnum(){
		return extnum;
	}

	/**  该通电话是否被应答  **/
	public void setIs_answered(int is_answered){
		this.is_answered=is_answered;
	}

	/**  该通电话是否被应答  **/
	public int getIs_answered(){
		return is_answered;
	}

	/**  坐席应答该通电话的时间;  **/
	public void setAnswered_time(Date answered_time){
		this.answered_time=answered_time;
	}

	/**  坐席应答该通电话的时间;  **/
	public Date getAnswered_time(){
		return answered_time;
	}

	/**  电话呼入时间  **/
	public void setCall_create_time(Date call_create_time){
		this.call_create_time=call_create_time;
	}

	/**  电话呼入时间  **/
	public Date getCall_create_time(){
		return call_create_time;
	}

	/**  挂机时间  **/
	public void setHangup_time(Date hangup_time){
		this.hangup_time=hangup_time;
	}

	/**  挂机时间  **/
	public Date getHangup_time(){
		return hangup_time;
	}

	/**  通话时长  **/
	public void setTime_len(int time_len){
		this.time_len=time_len;
	}

	/**  通话时长  **/
	public int getTime_len(){
		return time_len;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AcdEntity [phone_number=" + phone_number + ", uniqueid=" + uniqueid + ", groud_id=" + groud_id
				+ ", Id=" + Id + "]";
	}

	
}


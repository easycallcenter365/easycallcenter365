package com.telerobot.fs.utils;

import com.telerobot.fs.wshandle.MessageResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ValidatorUtil {
	
	/**
	 * 错误提示信息
	 */
	private String message = "";

	/**
	 * 是否允许为空
	 */
	private Boolean notNull = false;
	
	/**
	 * 是否必须为过去的日期
	 */
	private Boolean pastDate = false;
	
	/**
	 * 最大长度
	 */
	private Integer maxLen = null;
	
	/**
	 * 最小长度
	 */
	private Integer minLen = null;
	
	/**
	 * 验证正则表达式
	 */
	private String pattern = null;
	
	public ValidatorUtil(){}
	
	public ValidatorUtil(Boolean notNull, Integer maxLen, Integer minLen, String pattern) {
		super();
		this.notNull = notNull;
		this.maxLen = maxLen;
		this.minLen = minLen;
		this.pattern = pattern;
	}

	/**
	 * 执行验证
	 * @return
	 */
	public MessageResponse validate(String fieldName, String input){
		MessageResponse msg = new MessageResponse();;
		Boolean notPassed = false;
		if(null == input){
			input = "";
		}
		input = input.trim();
		if(notNull){
		   if(StringUtils.isNullOrEmpty(input)){
			   notPassed = true;
			   this.message = String.format("%s can't be null or empty.", fieldName);
		   }
		}
		
		if(!notPassed && null != maxLen){
			if(notNull || (!notNull && input.length() > 0 )) {
				if(input.length() > maxLen ){
					  notPassed = true;
					  this.message = String.format("length of %s can't exceed maxlength %d.", fieldName, maxLen);
				}
			}
		}
		
		if(!notPassed && null != minLen){
			if(notNull || (!notNull && input.length() > 0 )) {
				if(input.length() < minLen){
					  notPassed = true;
					  this.message = String.format("length of %s must be greater or equal to %d.", fieldName, minLen);
				}
			}
		}
		
		if(!notPassed && null != pattern){
			if(!StringUtils.isNullOrEmpty(input)){
				Pattern p = Pattern.compile(pattern); 
				Matcher m = p.matcher(input);
				Boolean matched = m.matches();
				if(!matched){
					  notPassed = true;
					  this.message = String.format("%s is invalid.", fieldName);
				}
			}
		}
		
		if(notPassed){
			msg.setStatus(400);
			msg.setMsg(this.message);
		}
		return msg;
	}

	public Boolean getNotNull() {
		return notNull;
	}

	public void setNotNull(Boolean notNull) {
		this.notNull = notNull;
	}

	public Boolean getPastDate() {
		return pastDate;
	}

	public void setPastDate(Boolean pastDate) {
		this.pastDate = pastDate;
	}

	public Integer getMaxLen() {
		return maxLen;
	}

	public void setMaxLen(Integer maxLen) {
		this.maxLen = maxLen;
	}

	public Integer getMinLen() {
		return minLen;
	}

	public void setMinLen(Integer minLen) {
		this.minLen = minLen;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	} 
	 
}

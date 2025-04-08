package com.telerobot.fs.entity.po;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SysParams implements Serializable {
  
	private static final long serialVersionUID = 3201030137426285599L;
	private String paramName;
	private String paramCode;
	private String paramValue;
	private int id;
	  
	public SysParams() {
	}

	public SysParams(String paramName, String paramCode, String paramValue) {
		this.paramName = paramName;
		this.paramCode = paramCode;
		this.paramValue = paramValue;
	}

	public String getParamName() {
		return this.paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamCode() {
		return this.paramCode;
	}

	public void setParamCode(String paramCode) {
		this.paramCode = paramCode;
	}

	public String getParamValue() {
		return this.paramValue;
	}

	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	} 
	
	
}

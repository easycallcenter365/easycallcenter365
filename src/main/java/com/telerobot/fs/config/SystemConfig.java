package com.telerobot.fs.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public  class SystemConfig {
	private static final Logger logger = LoggerFactory.getLogger(SystemConfig.class);

	/**
	 * 全局变量参数
	 */
	public static Map<String,String> paramsContext = new HashMap<String, String>();
	
	
	public static String  getValue(String key,String defaultValue){
		if(paramsContext.get(key) == null){
			return defaultValue;
		} else{
			return String.valueOf(paramsContext.get(key));
		}
	}
	
	public static String  getValue(String key){
		String destParamValue = String.valueOf(paramsContext.get(key));
		if(StringUtils.isEmpty(destParamValue)){
			logger.error("严重错误，无法获取到参数 {} 的值，请检查sys_params表！", key);
		}
		return destParamValue;
	}
	
	public static String  getValue(String key, String moduleName, boolean useModuleNameAsKey){
		return String.valueOf(paramsContext.get(moduleName + "_" +key));
	}
	
}
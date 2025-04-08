package com.telerobot.fs.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class RequestUtils {
	public static Map<String, Object> parameterValuesToMap(
			HttpServletRequest request) {
		  Map<String , Object> params = new HashMap<String, Object>();
	       Enumeration<String> paramNames = request.getParameterNames();
	       while (paramNames.hasMoreElements()) {
	           String paramName = paramNames.nextElement();
	           params.put(paramName, request.getParameter(paramName));
	       }
	       return params;
	}
	
	/**
	 *  获取客户端IP地址
	 */
	public static String getClientIp(HttpServletRequest  request) {
	        String remoteAddr = "";
	        if (request != null) {
	            remoteAddr = request.getHeader("X-FORWARDED-FOR");
	            if (remoteAddr == null || "".equals(remoteAddr)) {
	                remoteAddr = request.getRemoteAddr();
	            }
	        }
	        return remoteAddr;
	    }
}

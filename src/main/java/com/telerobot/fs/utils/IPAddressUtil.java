package com.telerobot.fs.utils;

public class IPAddressUtil {

	/**
	 *   检查是否为内网ip地址; 下面三类为内网地址  <BR/>
	 *    10.0.0.0/8：10.0.0.0～10.255.255.255 <BR/>
     *    172.16.0.0/12：172.16.0.0～172.31.255.255 <BR/>
     *    192.168.0.0/16：192.168.0.0～192.168.255.255  
	 **/
	public static boolean internalIp(String ip) {
	    if(ip.startsWith("10.") || ip.startsWith("192.168."))
	    	return true; 
	    if(ip.startsWith("172.")){
	    	int secondPart = Integer.valueOf(ip.split(".")[1]);
	    	if(secondPart >= 16 && secondPart <= 31) return true;
	    }
	    return false;
	} 

	 

	
}

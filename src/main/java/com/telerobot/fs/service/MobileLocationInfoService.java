package com.telerobot.fs.service;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/***
    * 号码归属地查询
    ***/
@Service 
public class MobileLocationInfoService {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Resource 
	private JdbcTemplate jdbcTemplate; 

	/***
	 * 根据七位手机号码前缀查询号码归属地 
	 ****/
	public String checkLocation(String mobilePartString, String areaInfo)
	{  
		String sql = "Select count(*) from mobile_location_info where MobileNumber='"+ mobilePartString +"' and MobileArea='"+ areaInfo +"'";
		try {
			return jdbcTemplate.queryForObject(sql, String.class);
		} 
		catch (Exception e) {
			log.info("查询手机号码归属地发生错误," + e.toString());
		}
		return "";
	}
	
	/***
	 * 根据网关对应表，匹配七位手机号码前缀查询号码归属地 
	 ****/
	public String checkLocationByGatename(String mobilePartString, String gatename)
	{  
		String sql = "Select count(1) from " + gatename + " where mobile = '" + mobilePartString + "'";
		try {
			return jdbcTemplate.queryForObject(sql, String.class);
		} 
		catch (Exception e) {
			log.info("查询手机号码归属地发生错误," + e.toString());
		}
		return "";
	}
        
}

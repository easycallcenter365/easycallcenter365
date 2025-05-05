package com.telerobot.fs.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.telerobot.fs.service.SysService;

/**
 * 服务启动执行
 */
@Component
@Order(value = 1)
public class MyStartupRunner implements CommandLineRunner {
	
	@Resource
	private SysService sysService;

	@Override
	public void run(String... args) throws Exception {
		Properties propFlies = new Properties();
		try {
			propFlies.load(getClass().getResourceAsStream(
					"/system.properties"));
			Map<String, String> resolvePrperties = new HashMap<String, String>();
			for (Object key : propFlies.keySet()) {
				String keyStr = key.toString();
				resolvePrperties.put(keyStr, propFlies.getProperty(keyStr));
			}
			PropertiesUtils.setProperties(resolvePrperties);
		} catch (Exception e) {
		}
		//刷新静态数据
		sysService.refreshParams();
	}

}
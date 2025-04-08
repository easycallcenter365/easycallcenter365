
package com.telerobot.fs.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * 系统配置文件读取工具类
 * @author: easycallcenter365@126.com
 *
 */
public class PropertiesUtils {

	/** 资源文件中配置的信息 */
	private static Map<String, String> properties = new HashMap<String, String>();

	/**
	 * 设置资源文件中的配置信息
	 * 
	 * @param properties
	 *            配置信息
	 */
	public static void setProperties(Map<String, String> properties) {
		PropertiesUtils.properties.putAll(properties);
	}

	public static Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * 根据KEY取得资源配置信息
	 * 
	 * @param key
	 *            key，资源文件中配置的key
	 * @return 资源配置信息
	 */
	public static String getProperty(String key) {

		String result = properties.get(key);
		if (StringUtils.isEmpty(result)) {
			result = "";
		}
		return result.trim();
	}

	public static String getProperty(String key, String defVal) {
		String result = properties.get(key);
		if (StringUtils.isEmpty(result)) {
			return defVal;
		}
		return result.trim();
	}

	/**
	 * 根据KEY删除资源文件中的配置信息
	 * 
	 * @param key
	 *            key，资源文件中配置的key
	 */
	public static void removeProperty(String key) {
		properties.remove(key);
	}

	/**
	 * 从配置文件读取字典数据,形如:0-新建,1-待审批,2-待生效...
	 * 
	 * @param key
	 * @return
	 */
	public static Map<String, String> getPropertyMap(String key) {
		return getPropertyMap(key, ",", "-");
	}

	public static Map<String, String> getPropertyMap(String key, String joiner, String pair) {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		if (StringUtils.isEmpty(key)) {
			return map;
		}
		String result = properties.get(key);
		if (StringUtils.isEmpty(result)) {
			return map;
		}
		String[] pairs = result.split(joiner);
		for (String _pair : pairs) {
			if (StringUtils.isEmpty(_pair)) {
				continue;
			}
			String[] kvs = _pair.split(pair);
			if (kvs.length == 2) {
				map.put(StringUtils.trim(kvs[0]), StringUtils.trim(kvs[1]));
			}
		}
		return map;
	}
	
	
	/** 
	 * 将驼峰式命名的字符串转换为下划线大写方式。如果转换前的驼峰式命名的字符串为空，则返回空字符串。</br> 
	 * 例如：HelloWorld->HELLO_WORLD 
	 * @param name 转换前的驼峰式命名的字符串 
	 * @return 转换后下划线大写方式命名的字符串 
	 */  
	public static String underscoreName(String name) {  
	    StringBuilder result = new StringBuilder();  
	    if (name != null && name.length() > 0) {  
	        // 将第一个字符处理成大写  
	        result.append(name.substring(0, 1).toUpperCase());  
	        // 循环处理其余字符  
	        for (int i = 1; i < name.length(); i++) {  
	            String s = name.substring(i, i + 1);  
	            // 在大写字母前添加下划线  
	            if (s.equals(s.toUpperCase()) && !Character.isDigit(s.charAt(0))) {  
	                result.append("_");  
	            }  
	            // 其他字符直接转成大写  
	            result.append(s.toUpperCase());  
	        }  
	    }  
	    return result.toString();  
	}  
	   
	/** 
	 * 将下划线大写方式命名的字符串转换为驼峰式。如果转换前的下划线大写方式命名的字符串为空，则返回空字符串。</br> 
	 * 例如：HELLO_WORLD->HelloWorld 
	 * @param name 转换前的下划线大写方式命名的字符串 
	 * @return 转换后的驼峰式命名的字符串 
	 */  
	public static String camelName(String name) {  
	    StringBuilder result = new StringBuilder();  
	    // 快速检查  
	    if (name == null || name.isEmpty()) {  
	        // 没必要转换  
	        return "";  
	    } else if (!name.contains("_")) {  
	        // 不含下划线，仅将首字母小写  
	        return name.substring(0, 1).toLowerCase() + name.substring(1);  
	    }  
	    // 用下划线将原始字符串分割  
	    String camels[] = name.split("_");  
	    for (String camel :  camels) {  
	        // 跳过原始字符串中开头、结尾的下换线或双重下划线  
	        if (camel.isEmpty()) {  
	            continue;  
	        }  
	        // 处理真正的驼峰片段  
	        if (result.length() == 0) {  
	            // 第一个驼峰片段，全部字母都小写  
	            result.append(camel.toLowerCase());  
	        } else {  
	            // 其他的驼峰片段，首字母大写  
	            result.append(camel.substring(0, 1).toUpperCase());  
	            result.append(camel.substring(1).toLowerCase());  
	        }  
	    }  
	    return result.toString();  
	}  
	

}

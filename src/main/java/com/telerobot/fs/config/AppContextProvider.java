package com.telerobot.fs.config;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import com.telerobot.fs.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component
@WebListener
public class AppContextProvider implements ApplicationListener<ApplicationReadyEvent> {

	private final static Logger logger = LoggerFactory.getLogger(AppContextProvider.class);

	private static ApplicationContext ctx = null;

	private static ServletContext sc = null;
	
	public static String CONTEXT_PATH = "";
	

	public static ApplicationContext getApplicationContext() {
		return ctx;
	}
	
	public static ServletContext getServletContext() {
		return sc;
	}

	
	private static void extracted(ApplicationContext applicationContext) {
		AppContextProvider.ctx = applicationContext;

	}
	
	
	 /**
     * 获取指定beanname的bean
     * @param bean 设定后的bean的name
     * @param clazz 接口的class
     * @return
     */
    public static <T> T getBean(String bean, Class<T> clazz)
    {
        return ctx.getBean(bean, clazz);
    }
    
    /**
     * bean名称默认为全限定名称，所以可以不用beanname参数
     * @param clazz 接口的class
     * @return
     */
    public static <T> T getBean(Class<T> clazz)
    {
        return ctx.getBean(clazz);
    }
    /**
     * bean名称默认为全限定名称，所以可以不用beanname参数
     * @return
     */
    public static <T> T getBean(String bean)
    {
        return (T) ctx.getBean(bean);
    }



	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
    	logger.info("on application event...");
		extracted(applicationReadyEvent.getApplicationContext());
	}

	public static void setApplicationContext(ApplicationContext context){
		logger.info("set  applicationContext ....");
		extracted(context);
	}

	/**
	 *  获取 application.properties的配置信息；
	 * @param key 指定的key
	 * @return
	 */
	public static String getEnvConfig(String key){
		Environment env = ctx.getEnvironment();
		String value = env.getProperty(key);
		if(StringUtils.isNullOrEmpty((value))){
			logger.warn("配置文件中缺少参数: {}", key);
		}
		return  value;
	}

	/**
	 *  获取 application.properties的配置信息；
	 * @param key 指定的key
	 * @return
	 */
	public static String getEnvConfig(String key, String defaultValue){
		Environment env = ctx.getEnvironment();
		String destValue = env.getProperty(key);
		if(StringUtils.isNullOrEmpty((destValue))){
			logger.warn("配置文件中缺少参数: {}", key);
		}
		return StringUtils.isNullOrEmpty(destValue) ? defaultValue : destValue ;
	}

}

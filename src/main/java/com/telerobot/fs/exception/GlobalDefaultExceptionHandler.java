package com.telerobot.fs.exception;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalDefaultExceptionHandler {
	
	private static final Log LOG = LogFactory.getLog(GlobalDefaultExceptionHandler.class);

	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public Map<String, Object>  defaultErrorHandler(HttpServletRequest req, Exception e) {
		Map<String, Object> result = new HashMap<String, Object>();
		String msg = e.getMessage();
		if (StringUtils.isEmpty(msg)) {
			msg = "系统内部错误,请联系管理员!";
		}
		LOG.error(msg, e);
		result.put("msg", msg);
		result.put("detail", ExceptionUtils.getStackTrace(e));
		return result;

		// // If the exception is annotatedwith @ResponseStatus rethrow it and
		// let

		// // the framework handle it -like the OrderNotFoundException example

		// // at the start of thispost.

		// // AnnotationUtils is aSpring Framework utility class.

		// if (AnnotationUtils.findAnnotation(e.getClass(),ResponseStatus.class)
		// != null)

		// throw e;

		//

		// // Otherwise setup and sendthe user to a default error-view.

		// ModelAndView mav =new ModelAndView();

		// mav.addObject("exception", e);

		// mav.addObject("url",req.getRequestURL());

		// mav.setViewName(DEFAULT_ERROR_VIEW);

		// return mav;

		// 打印异常信息：
		/*
		 * 
		 * 返回json数据或者String数据：
		 * 
		 * 那么需要在方法上加上注解：@ResponseBody
		 * 
		 * 添加return即可。
		 */

		/*
		 * 
		 * 返回视图：
		 * 
		 * 定义一个ModelAndView即可，
		 * 
		 * 然后return;
		 * 
		 * 定义视图文件(比如：error.html,error.ftl,error.jsp);
		 */

	}

}
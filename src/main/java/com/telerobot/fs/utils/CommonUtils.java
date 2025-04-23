package com.telerobot.fs.utils;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.po.FunAsrResultEntity;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.RespStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 工具类
 */
public class CommonUtils<T>  {
	private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

	public static Map<String, String> parseUrlQueryString(String queryString) throws UnsupportedEncodingException {
		Map<String, String> queryPairs = new HashMap<>(16);
		String[] pairs = queryString.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
			String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
			queryPairs.put(key, value);
		}
		return queryPairs;
	}

	private static final String FUN_ASR_MODE_ONLINE = "2pass-online";
	private static final String FUN_ASR_MODE_OFFLINE = "2pass-offline";
	public static FunAsrResultEntity parseFunAsrResponse(String msg){
		FunAsrResultEntity resultEntity = new FunAsrResultEntity();
		JSONObject jsonObject = JSONObject.parseObject(msg);
		boolean isFinal = jsonObject.getBoolean("is_final");
		resultEntity.setFinal_flag(isFinal);
		String mode = jsonObject.getString("mode");
		String text = jsonObject.getString("text");
		String vadType = "";
		if(!StringUtils.isEmpty(text)){
			if(mode.equals(FUN_ASR_MODE_ONLINE)) {
				vadType = "middle";
			}else if(mode.equals(FUN_ASR_MODE_OFFLINE)) {
				vadType = "vad";
			}
			resultEntity.setVad_type(vadType);
			resultEntity.setText(text);
			return resultEntity;
		}
		return null;
	}

	public static String joinTtsFiles(String traceId, String ttsFiles, boolean with_prefix, boolean checkExists){
		StringBuilder ttsFileUnion = new StringBuilder();
		if(!StringUtils.isEmpty(ttsFiles)){
			if(ttsFiles.contains(";")) {
				String[] fileArrs = ttsFiles.split(";");
				if(with_prefix) {
					ttsFileUnion.append("file_string://");
				}
				for (int i = 0; i <= fileArrs.length - 1; i++) {
					if (checkExists && !new File(fileArrs[i]).exists()) {
						logger.error("{} 录音文件不存在，跳过放音: {} ", traceId, fileArrs[i]);
					}else{
						if(i != fileArrs.length - 1){
							ttsFileUnion.append(fileArrs[i]).append("!");
						}else{
							ttsFileUnion.append(fileArrs[i]);
						}
					}
				}
			}else{
				// 只有一个wav文件;
				ttsFileUnion.append(ttsFiles);
			}
		}
		return ttsFileUnion.toString();
	}

	public static String getStackTraceString(StackTraceElement[] stackTraceElements){
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < stackTraceElements.length; i++) {
			stringBuilder.append("ClassName:");
			stringBuilder.append(stackTraceElements[i].getClassName());
			stringBuilder.append("\n FileName:");
			stringBuilder.append(stackTraceElements[i].getFileName());
			stringBuilder.append("\n LineNumber:");
			stringBuilder.append(stackTraceElements[i].getLineNumber());
			stringBuilder.append("\n MethodName:");
			stringBuilder.append(stackTraceElements[i].getMethodName());
		}
		return stringBuilder.toString();
	}

	/**
	 *  已知的拨号错误
	 */
	public static  String[] KNOWN_DIAL_FAIL_CASE_TABLES = new String[]{
			"DESTINATION_OUT_OF_ORDER",
			"NO_ANSWER",
			"USER_BUSY",
			"NO_USER_RESPONSE",
			"RECOVERY_ON_TIMER_EXPIRE",
			"INCOMPATIBLE_DESTINATION"
	};

	/**
	 * 检测拨号错误是否命中已知的错误类型
	 * @param callResponseStr
	 * @return 命中返回true，否则false
	 */
	public static boolean checkTransferFailCase(String callResponseStr){
		boolean hitCase = false;
		for (String caseStr : KNOWN_DIAL_FAIL_CASE_TABLES) {
			if(callResponseStr.contains(caseStr)){
				hitCase = true;
				break;
			}
		}
		return hitCase;
	}

	/**
	 *  解析外呼时的分机错误;
	 * @param callResponseStr
	 * @param extnum
	 * @return
	 */
	public static MessageResponse  sendExtensionErrorInfo(String callResponseStr, String extnum) {
		MessageResponse response = null;
		if (callResponseStr.contains("USER_BUSY")) {
			response = (new MessageResponse(RespStatus.CALLER_BUSY, "呼叫失败，分机忙，请先挂断上一通电话。"));
		} else if (callResponseStr.contains("USER_NOT_REGISTERED")) {
			response = (new MessageResponse(RespStatus.CALLER_NOT_LOGIN, "分机没有登录,请打开软电话，确保软电话号码是" + extnum));
		} else if (callResponseStr.contains("SUBSCRIBER_ABSENT")) {
			response = (new MessageResponse(RespStatus.CALLER_NOT_LOGIN, "分机没有登录,请打开软电话，确保软电话号码是" + extnum));
		} else if (callResponseStr.contains("NO_USER_RESPONSE")) {
			response = (new MessageResponse(RespStatus.CALLER_RESPOND_TIMEOUT, "分机无响应，请重新打开电话或者刷新页面后重试。"));
		} else if (callResponseStr.contains("RECOVERY_ON_TIMER_EXPIRE")) {
			response = (new MessageResponse(RespStatus.CALLER_RESPOND_TIMEOUT, "操作超时，请检查分机是否已经登录，稍后重试。"));
		} else if (callResponseStr.contains("NO_ANSWER")) {
			response = (new MessageResponse(RespStatus.CALLER_RESPOND_TIMEOUT, "外呼超时"));
		} else if (callResponseStr.contains("INCOMPATIBLE_DESTINATION")) {
			response = (new MessageResponse(RespStatus.SERVER_ERROR_AUDIO_CODEC_NOT_MATCH, "外呼失败，可能是语音编码不匹配。 INCOMPATIBLE_DESTINATION"));
		} else if (callResponseStr.trim().length() != 0) {
			response = (new MessageResponse(RespStatus.SERVER_ERROR, "操作失败，请稍后重试，详情：" + callResponseStr));
		}
 		return response;
	}

	public static  Map<String,String> validateToken(String token, String traceId){
		try {
			//创建验证对象,这里使用的加密算法和密钥必须与生成TOKEN时的相同否则无法验证
			JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(
					SystemConfig.getValue("ws-server-auth-token-secret").trim()
			)).build();
			//验证JWT
			DecodedJWT decodedJwt = jwtVerifier.verify(token);
			Map<String,String> map = new HashMap<>(10);
			map.put("extnum", decodedJwt.getClaim("extnum").asString());
			map.put("opnum", decodedJwt.getClaim("opnum").asString());
			map.put("groupId", decodedJwt.getClaim("groupId").asString());
			map.put("skillLevel", decodedJwt.getClaim("skillLevel").asString());
			map.put("calleePrefix", decodedJwt.getClaim("calleePrefix").asString());
			map.put("callerNumber", decodedJwt.getClaim("callerNumber").asString());
			map.put("gatewayAddress", decodedJwt.getClaim("gatewayAddress").asString());
			map.put("projectId", decodedJwt.getClaim("projectId").asString());
			map.put("sipProfile", decodedJwt.getClaim("sipProfile").asString());
			return map;
			//获取JWT中的数据,注意数据类型一定要与添加进去的数据类型一致,否则取不到数据
			// System.out.println(decodedJwt.getExpiresAt());
		} catch (Throwable err) {
			logger.warn("{} token 校验失败: {}", traceId, err.toString());
			return null;
		}
	}
	
	/**
	 * 禁止jsp页面被客户端浏览器缓存
	 * @param response
	 */
	public static void setPageNoCache(HttpServletResponse response){
		response.setHeader("Pragma","no-cache"); 
		response.setHeader("Cache-Control","no-cache"); 
		response.setDateHeader("Expires", 0); 
	}

	public static String hiddenPhoneNumber(String phone) {
		if (phone == null || phone.length() == 0) {
			return "";
		}
		if (phone.length() < 10) {
			return phone;
		}
		return phone.substring(0, 3) + "****" + phone.substring(7, phone.length());
	}

	/**
	 * 设置页面缓存为指定的分钟数后过期
	 * @param response
	 * @param minutes
	 */
	public static void setPageCacheByTime(HttpServletResponse response, int minutes){
		Date d = new Date(); 
		String modDate = d.toGMTString(); 
		String expireDate = (new Date(d.getTime() + minutes * 60 * 1000)).toGMTString(); 
		response.setHeader("Last-Modified", modDate); 
		response.setHeader("Expires", expireDate); 
		response.setHeader("Cache-Control", "public");  
	}
	
	
    /**
     * 过滤数组中重复的元素
     * 
     * @param array 输入需要去重的数组
     * @return 返回去重后数组 
     */
    public List<T> uniqueArray(T[] array) {
        if (array.length == 0) {
            return null;
        }
		List<T> list =  new ArrayList<T>();;
        int length = array.length;
        for (int i = 0, len = length; i < len; i++) {
            if (!list.contains(array[i])) {
            	list.add(array[i]);
            }
        }
        return list;
    }
    
    public static void main(String[] args) {
    	String[] array = new String[]{"a","b","a","c","d"};
		List<String> returnList = new CommonUtils<String>().uniqueArray(array);
		System.out.println(returnList);
		
		Integer[] array2 = new Integer[]{1,2,3,4,4,2,2,1,1};
		List<Integer> returnList2 = new CommonUtils<Integer>().uniqueArray(array2);
		System.out.println(returnList2);
	}

    public static Boolean createZipFile(String sourceFile, String zipFilePath) {
    	try{
	        File file = new File(zipFilePath);
	        if(!file.exists())
	        file.createNewFile();
	        String string= FileUtils.ReadFile(sourceFile, "utf-8");
	        byte[] buffer =string.getBytes();
	        FileOutputStream fOutputStream = new FileOutputStream(file);
	        ZipOutputStream zoutput = new ZipOutputStream(fOutputStream);
	        ZipEntry zEntry  = new ZipEntry(new File(sourceFile).getName());
	        zoutput.putNextEntry(zEntry);
	        zoutput.write(buffer);
	        zoutput.closeEntry();
	        zoutput.close();
	        return true;
    	}
    	catch(Throwable e){
    		return false;
    	}
      }

	/**
	 * 格式化double类型为2位小数
	 * 
	 * @return
	 */
	public static Double formatDoubleWithTwo(Double f) {
		BigDecimal bg = new BigDecimal(f);
		return bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	/**把http请求参数转换为Map
	 * 
	 * @param request
	 * @return
	 */
	public static Map<String, String> getHttpRequestHeaders(HttpServletRequest request) {
		Map<String, String> map = new HashMap<String, String>();
		@SuppressWarnings("rawtypes")
		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, String> processRequestParameter(String data) {
		Map<String, String> params = new HashMap<String, String>();
		String[] keyValues;
		if (data.indexOf("&") != -1) {
			keyValues = data.split("&");
		} else {
			keyValues = new String[] { data };
		}
		for (String item : keyValues) {
			if (item.indexOf("=") != -1) {
				String[] tmp = item.split("=");
				if (tmp.length != 0) {
					if (tmp.length == 1) {
						params.put(tmp[0], "");
					}
					if (tmp.length == 2) {
						params.put(tmp[0], tmp[1]);
					}
				}
			}
		}
		return params;
	}

	/**
	 * Integer转为long
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月17日 上午10:17:26
	 */
	public static Long integer2long(Object data) {
		try {
			return ((Integer) (data == null ? new Integer(0) : data)).longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BigInteger转为long
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月16日 上午10:50:31
	 */
	public static Long biginteger2long(Object data) {
		try {
			return ((BigInteger) (data == null ? new BigInteger("0") : data)).longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BigInteger转为int
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月19日 下午1:41:19
	 */
	public static Integer biginteger2int(Object data) {
		try {
			return ((BigInteger) (data == null ? new BigInteger("0") : data)).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BigDecimal转为Double
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月16日 上午10:31:47
	 */
	public static Double bigdecimal2double(Object data) {
		try {
			return ((BigDecimal) (data == null ? BigDecimal.valueOf(0.0) : data)).doubleValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BigDecimal转为long
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月17日 上午10:25:27
	 */
	public static Long bigdecimal2long(Object data) {
		try {
			return ((BigDecimal) (data == null ? BigDecimal.valueOf(0.0) : data)).longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BigDecimal转为int
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月19日 下午1:44:58
	 */
	public static Integer bigdecimal2int(Object data) {
		try {
			return ((BigDecimal) (data == null ? BigDecimal.valueOf(0.0) : data)).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * String转为Long
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月29日 下午4:11:58
	 */
	public static Long string2long(String data) {

		try {
			return (data == null || "".equals(data)) ? null : Long.parseLong(data);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * String转为Double
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2016年12月29日 下午4:11:58
	 */
	public static Double string2double(String data) {

		try {
			return (data == null || "".equals(data)) ? 0.0 : Double.parseDouble(data);
		} catch (Exception e) {
			e.printStackTrace();
			return 0.0;
		}
	}

	public static boolean string2boolean(String data) {
		try {
			return (data == null || "".equals(data)) ? false : Boolean.valueOf(data);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 转为Date
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2017年2月4日 下午1:34:53
	 */
	public static Date obj2date(Object data) {

		try {
			return (data == null || "".equals(data)) ? null : (Date) (data);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 转为int
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2017年2月8日 下午4:24:46
	 */
	public static Integer obj2int(Object data, int defalutInt) {

		try {
			return (data == null || "".equals(data)) ? defalutInt : (Integer) (data);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 转为double
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @return
	 * @date: 2017年2月8日 下午4:24:46
	 */
	public static Double obj2double(Object data, double defalutDouble) {

		try {
			return (data == null || "".equals(data)) ? defalutDouble : (Double) (data);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 转为string
	 * 
	 * @author: easycallcenter365@126.com
	 * @param data
	 * @param defalutStr
	 * @return
	 * @date: 2017年2月8日 下午4:30:19
	 */
	public static String obj2string(Object data, String defalutStr) {

		try {
			return (data == null || "".equals(data)) ? defalutStr : (String) (data);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 转为bigdecimal
	 * 
	 * @author: easycallcenter365@126.com
	 * @param value
	 * @return
	 * @date: 2017年2月26日 下午3:05:13
	 */
	public static BigDecimal obj2bigdecimal(Object value) {
		BigDecimal ret = null;
		if (value != null) {
			if (value instanceof BigDecimal) {
				ret = (BigDecimal) value;
			} else if (value instanceof String) {
				ret = new BigDecimal((String) value);
			} else if (value instanceof BigInteger) {
				ret = new BigDecimal((BigInteger) value);
			} else if (value instanceof Number) {
				ret = BigDecimal.valueOf(((Number) value).doubleValue());
			} else {
				throw new ClassCastException("Not possible to coerce [" + value + "] from class " + value.getClass()
						+ " into a BigDecimal.");
			}
		}
		return ret;
	}

	/**
	 * 获取挂断类型
	 * 
	 * @author: easycallcenter365@126.com
	 * @param typeId
	 * @return
	 * @date: 2016年12月4日 下午3:11:10
	 */
	public static String getHangupType(Integer typeId) {
		if (null == typeId || typeId < 0) {
			return "";
		} else if (typeId == 0) {
			return "振铃挂断";
		} else if (typeId == 1) {
			return "坐席挂断";
		} else if (typeId == 2) {
			return "客户挂断";
		} else {
			return "";
		}
	}

	/**
	 * 将秒转为时分秒
	 * 
	 * @author: easycallcenter365@126.com
	 * @param t
	 *            单位秒
	 * @return hh:mm:ss
	 * @date: 2017年1月24日 上午9:42:46
	 */
	public static String formatTime(Long t) {
		String str = "";
		if (null == t || t <= 0) {
			return "0:0:0";
		}
		str = t / 3600 + ":" + (t % 3600) / 60 + ":" + t % 60;
		return str;
	}

	/**
	 * 将秒转为时分秒
	 * 
	 * @author: easycallcenter365@126.com
	 * @param t
	 *            单位秒
	 * @return hh:mm:ss
	 * @date: 2017年1月24日 上午9:42:46
	 */
	public static String formatTime2(Long t) {
		String str = "";
		if (null == t || t <= 0) {
			return "0:0:0";
		}
		str += t / 3600 + ":";
		if ((t % 3600) / 60 < 10) {
			str += "0" + (t % 3600) / 60 + ":";
		} else {
			str += (t % 3600) / 60 + ":";
		}
		if (t % 60 < 10) {
			str += "0" + t % 60;
		} else {
			str += t % 60;
		}
		return str;
	}

	/**
	 * 电话号码隐藏中间部分
	 * 
	 * @author: easycallcenter365@126.com
	 * @param num
	 * @return
	 * @date: 2017年3月16日 下午2:43:53
	 */
	public static String formatPhoneNum(String num) {
		int sub_start = 3;
		int sub_end = num.length() - 4;
		String hide_str = "";
		for (int i = 0; i < (sub_end - sub_start); i++) {
			hide_str += "*";
		}
		if (sub_start > sub_end) {
			return num;
		} else {
			return num.substring(0, sub_start) + hide_str + num.substring(sub_end, num.length());
		}
	}

	/**
	 * 比较2个list是否相同（逗号隔开）
	 * 
	 * @author: easycallcenter365@126.com
	 * @time 下午2:35:10
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static boolean equalsList(String list1, String list2) {
		if (null == list1 || "".equals(list1)) {
			return false;
		}
		if (null == list2 || "".equals(list2)) {
			return false;
		}
		if (list1.split(",").length != list2.split(",").length) {
			return false;
		}
		for (String obj : list1.split(",")) {
			if (list2.indexOf(obj) < 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 从HttpServletRequest参数中获取用户请求参数值
	 * 
	 * @param request
	 * @param key
	 * @return
	 */
	public static int getRequestParameterInt(HttpServletRequest request, String key) {
		String idValue = request.getParameter(key);
		if (idValue != null && idValue.trim() != "") {
			return Integer.valueOf(idValue);
		}
		return 0;
	}

	/**
	 * 从HttpServletRequest参数中获取用户请求参数值
	 * 
	 * @param request
	 * @param key
	 * @return
	 */
	public static String getRequestParameterStr(HttpServletRequest request, String key) {
		String idValue = request.getParameter(key);
		if (idValue != null && idValue.trim() != "") {
			return String.valueOf(idValue);
		}
		return "";
	}

	/**
	 * 格式化idList为List<Integer> 类型的对象;
	 * 
	 * @param idListStr
	 *            例如"521,352,523"
	 * @return
	 */
	public static List<Integer> parseIdList(String idListStr) {
		if (!StringUtils.isNotBlank(idListStr))
			return null;
		List<Integer> list = new ArrayList<Integer>(10);
		String[] array = idListStr.split(",");
		for (String ele : array) {
			list.add(Integer.valueOf(ele));
		}
		return list;
	}

	/**
	 * uuid的正则表达式
	 */
	public static final String uuidRegExpPattern = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

	public static String ListToString(List<?> objectList) {
		if(objectList.size() == 0) {
			return "";
		}
		return ListToString(objectList, true);
	}

	/**
	 * ListToString,是否使用逗号分隔符
	 ***/
	public static String ListToString(List<?> objectList, boolean useSpe) {
		StringBuilder sb = new StringBuilder("");
		for (Object ele : objectList) {
			sb.append(ele);
			if (useSpe) {
				sb.append(",");
			}
		}
		String result = sb.toString();
		if (useSpe) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	 /* 把计费周期转换为对应的秒数
	 * @param period
	 * @return
	 */
	public static Long calcRentTime(Integer rentType){
		Long secs = 0L;
		switch(String.valueOf(rentType)){
		   case "0":
			   secs = 24L * 3600L; //天
		   break;
		   case "1":
			   secs = 30L* 24L * 3600L; //月
		   break;
		   case "2":
			   secs = 360L * 30L* 24L * 3600L; //年
		   break;
		   
		}
		return secs;
	}
}

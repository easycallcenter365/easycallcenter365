package com.telerobot.fs.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.telerobot.fs.wshandle.MessageResponse;

public class RegExp {

	/***
	 * 从文本中用正则表达式搜索匹配文本
	 * 
	 * @param _Text
	 *            待搜索文本
	 * @param _RegExp
	 *            正则表达式
	 * @return 返回匹配结果的List
	 ***/
	public static List<String> GetMatchFromStringByRegExp(String _Text, String _RegExp) {
		Pattern p = Pattern.compile(_RegExp);
		Matcher m = p.matcher(_Text);
		List<String> _MatchsResult = new ArrayList<String>(5);
		while (m.find()) {
			_MatchsResult.add(m.group());
		}
		return _MatchsResult;
	}

	/***
	 * 查找指定字符第n次出现的位置
	 ***/
	public static int SearchCharNAppearPos(String str, char search, int n) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == search) {
				count += 1;
				if (n == count) {
					return i;
				}
			}
		}
		return 0;
	}

	/***
	 * 查找指定字符第n次出现的位置
	 ***/
	public static int SearchCharNAppearPosEx(String str, char search, int n) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == search) {
				count += 1;
				if (n == count) {
					return i;
				}
			}
		}
		if (n != count) {
			return -1;
		}
		return -1;
	}


	/**
	 * 全球手机号验证 (判断是否为6-20位的数字)
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse isMobile(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(20);
		util.setMinLen(6);
		util.setNotNull(true);
		util.setPattern("^[1-9]\\d{5,19}$");
		String fieldName = "mobile";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 校验id是否合法 (id为21位数字)
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkId(Boolean isRequired, String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(21);
		util.setMinLen(21);
		util.setNotNull(isRequired);
		util.setPattern(regExp_IdUnique);
		String fieldName = "order_id";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 校验电话卡是否合法 (电话卡为20位数字)
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkPhoneCard(Boolean isRequired, String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(20);
		util.setMinLen(20);
		util.setNotNull(isRequired);
		util.setPattern("^\\d{20}$");
		String fieldName = "card_code";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 校验id是否合法 (id为1-21位的数字)
	 * 
	 * @param checkValue
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkIdDigit(Boolean isRequired, String checkValue, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(21);
		util.setMinLen(1);
		util.setNotNull(isRequired);
		util.setPattern("^[1-9]\\d{0,20}$");
		String fieldName = "id";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, checkValue);
	}

	/**
	 * 校验是否为有效的时间戳
	 * 
	 * @param checkValue
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkTimeStamp(Boolean isRequired, String checkValue, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(13);
		util.setMinLen(1);
		util.setNotNull(isRequired);
		util.setPattern(regExp_TimeStamp); // 时间戳可以为0，或者13位数字;
		String fieldName = "timestamp";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, checkValue);
	}

	/**
	 * 校验是否为有效的产品类型
	 * 
	 * @param checkValue
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkProductType(Boolean isRequired, String checkValue, String... fieldNameArgs) {
		MessageResponse msg = new MessageResponse();
		if(!isRequired && StringUtils.isNullOrEmpty(checkValue)){
			return msg;
		}
		Class<?> clazz = null;
		try {
			clazz = Class.forName("com.cullian.common.dto.product.SysProductType");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		boolean matched = false;
		if (null != clazz) {
			Field[] fields = clazz.getFields();
			for (Field field : fields) {
				try {
					String value = (String) field.get(clazz);
					if (checkValue.equalsIgnoreCase(value)){
						matched = true;
						break;
					}
				} catch (Throwable e) {
				}
			}
			
		}
		if(!matched) {
			msg.setStatus(400);
			msg.setMsg("invalid productType " + checkValue);
		}
		return msg;
	}

	public static MessageResponse checkPayChannel(String channelName) {
		MessageResponse msg = new MessageResponse();
		if (!"paypal".equalsIgnoreCase(channelName) && !"payssion".equalsIgnoreCase(channelName)
				&& !"adyen".equalsIgnoreCase(channelName)) {
			msg.setStatus(400);
			msg.setMsg("invalid_paychannel, expect 'paypal'、 'payssion' or 'adyen'  ");
		}
		return msg;
	}

	/**
	 * 判断是否为合法的转账金额 (最大支持999.99美金)
	 * 
	 * @param str
	 * @param fieldNameArgs
	 * @return
	 */
	public static MessageResponse isAmountMoney(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setNotNull(true);
		util.setPattern("^([1-9]\\d{0,2}|0)([.]?|(\\.\\d{1,2})?)$");
		String fieldName = "amount";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 判断是否为32位的MD5字符串
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse isMd5String(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(32);
		util.setMinLen(32);
		util.setNotNull(true);
		util.setPattern("^[0-9a-fA-F]{32}$");
		String fieldName = "device_id";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 判断是否为Freeswitch的uuid
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkFreeswitchUUID(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(36);
		util.setMinLen(36);
		util.setNotNull(true);
		util.setPattern("^([0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12})$");
		String fieldName = "uuid";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	/**
	 * 国家码验证 (判断是否为1-6位的数字)
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static boolean isAreaCode(String str) {
        return checkRegExp(str, regExp_AreaCode);        
	}
	
	/**
	 * did号码验证
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static boolean isDidNumber(String str) {
        return checkRegExp(str, regExp_DidNumber);        
	}
	
	/**
	 * 日期验证
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static boolean isDate(String str) {
		return checkRegExp(str, regExp_Date);
	}
	
	public static boolean checkRegExp(String input ,String regExp){
		Pattern p = null;
		Matcher m = null;
		boolean b = false;
		p = Pattern.compile(regExp);
		m = p.matcher(input);
		b = m.matches();
		return b;
	}
	
	
	
	/**
	 * 价格验证
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static boolean isPrice(String str) {
        return checkRegExp(str, regExp_Price);        

	}
	
	public static MessageResponse isPriceEx(String str, boolean isRequired,   String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(7);
		util.setMinLen(1);
		util.setNotNull(isRequired);
		util.setPattern(regExp_Price);
		String fieldName = "price";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}

	
	/**
	 * 验证话单的月份(yyyyMM)
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse isCdrMonth(String str, Boolean required, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(6);
		util.setMinLen(6);
		util.setNotNull(required);
		util.setPattern(regExp_CdrMonth);
		String fieldName = "month";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}
	
	/**
	 * 验证储值卡的卡号是否合法
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkStoredValueCard(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(20);
		util.setMinLen(20);
		util.setNotNull(true);
		util.setPattern(regExp_StoredValueCard);
		String fieldName = "card_code";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return  util.validate(fieldName, str);
	}
	
	
	
	/**
	 * did号码验证
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse isDidNumberEx(String str, String... fieldNameArgs) {
		MessageResponse msg = new MessageResponse();
		ValidatorUtil util = new ValidatorUtil();
		util.setMaxLen(12);
		util.setMinLen(5);
		util.setNotNull(true);
		util.setPattern(regExp_DidNumber);
		String fieldName = "did_number";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}
	
	/**
	 * 21位数字的id正则
	 */
	public static final String regExp_IdUnique = "^[1-9]\\d{20}$";
	
	/**
	 * 自增主键id正则
	 */
	public static final String regExp_IdPrimaryKey = "^[1-9]\\d{0,9}$";
	
	/**
	 * 国家码正则
	 */
	public static final String regExp_AreaCode = "^[1-9]\\d{0,5}$";
	
	/**
	 * 电话卡卡号正则
	 */
	public static final String regExp_StoredValueCard = "^\\d{20}$";
	
	/**
	 * 计费号码段正则
	 */
	public static final String regExp_NumberSection = "^(\\d{2,12})$";
	
	/**
	 * 时间戳(允许为0)
	 */
	public static final String regExp_TimeStamp = "^([0]|\\d{13})$";

	/**
	 * 时间戳(不允许为0)
	 */
	public static final String regExp_TimeStampEx = "^([1-9]\\d{12})$";
	
	/**
	 * 日期正则，匹配本世纪的日期;
	 * 需要辅以 DateSimpleFormat进行转换判断; 
	 */
	public static final String regExp_Date = "^(20\\d{2})(\\-)(0{0,1}[1-9]|1[1-2])(\\-)([0-2]{0,1}[1-9]|[3][0-1])";
	
	/**
	 * did号码正则
	 */
	public static final String regExp_DidNumber = "^(\\d{5,12})$";
	
	/**
	 * 运营商名称正则
	 */
	public static final String regExp_SpName = "^([\\s\\S]){1,50}$";
	
	/**
	 * 备注正则
	 */
	public static final String regExp_memo = "^([\\s\\S]){2,100}$";
	
	/**
	 * 用户名正则
	 */
	public static final String regExp_UserName = "^[a-zA-Z0-9_-]{3,50}$";
	
	/**
	 * 费率格式正则
	 */
	public static final String regExp_FeeRate =  "^([0])+(.[0-9]{8})?$";
	
	
	/**
	 * (did号码和套餐的正则，不超过) 价格正则
	 */
	public static final String regExp_Price =  "^([0-9]{1,3})+(.[0-9]{0,2})?$";
	
	/**
	 * 转义字符
	 */
	public static final String regExp_EscapeCharacter = "[\\'\\\\\\/\\b\\f\\n\\r\\t]";

	/**
	 * 话单的月份正则
	 */
	public static final String regExp_CdrMonth = "^20\\d{2}(([1][0-2])|([0][1-9]))$";

	public static final String regExp_IpAddress = "^((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))$";

	/**
	 * 检测ip地址是否合法;
	 *  比如： 192.168.66.70
	 * @param ip
	 * @return
	 */
	public static boolean checkIpAddress(String ip){
        return checkMatchByPattern(ip, regExp_IpAddress);
	}

	/**
	 * 检测端口是否合法;
	 * @param port
	 * @return
	 */
	public static boolean checkPort(int port) {
           return port > 0 && port <= 65535;
	}

	/**
	 *  检测ip:port组合是否合法;
	 *  比如： 192.168.66.70:5060
	 * @param address
	 * @return
	 */
	public static boolean checkIpAddressWithPort(String address) {
		if(StringUtils.isNullOrEmpty(address)){
			return  false;
		}
		String split = ":";
		if (!address.contains(split)) {
			return false;
		}
		String[] array = address.split(split);
		if (array.length != 2 || !StringUtils.isNumeric(array[1].trim())) {
			return false;
		}
		return checkPort(Integer.parseInt(array[1].trim())) &&
				checkIpAddress(array[0].trim());
	}

	public static void main(String[] args) {
		System.out.println(checkIpAddressWithPort("8.8.8.70：65530"));
	}

	/**
	 *  检测指定的输入是否符合正则表达式模式匹配;
	 * @param input
	 * @param pattern
	 * @return
	 */
	public static boolean checkMatchByPattern(String input, String pattern){
		if(!StringUtils.isNullOrEmpty(input)){
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(input.trim());
			return m.matches();
		}
		return  false;
	}

	/**
	 * 检查昵称是否正确
	 * 
	 * @param str
	 * @return 验证通过返回true
	 */
	public static MessageResponse checkNickName(String str, String... fieldNameArgs) {
		ValidatorUtil util = new ValidatorUtil(true, 50, 2, null);
		String fieldName = "nick_name";
		if (fieldNameArgs != null && fieldNameArgs.length != 0) {
			fieldName = fieldNameArgs[0];
		}
		return util.validate(fieldName, str);
	}



}

package com.telerobot.fs.utils;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.entity.pojo.BaseMessage;

public class ResponseUtil {

	public static String toJson(String header, String content, Object datas)
	{
		BaseMessage msg = new BaseMessage(header, content, datas);
		return JSON.toJSONString(msg);
	}
}

package com.telerobot.fs.wshandle;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载ActionType信息，并对外提供一个工具方法
 * 
 * @author easycallcenter365@gmail.com
 * @date 2018年11月5日 上午10:44:44
 */
public class MsgActionTypeUtil {
	public MsgActionTypeUtil() {}
	private static List<MsgActionType> actionTypeList = new ArrayList<MsgActionType>(10);
	private static Object lockHelper = new Object();
	/**
	 * 从数据库加载ActionType信息
	 */
	private static void loadActionTypeInfoFromDb() {
		if (actionTypeList.size() == 0) {
			synchronized (lockHelper) {
				if (actionTypeList.size() == 0){
					String wsHandleList = AppContextProvider.getEnvConfig("sys-config.websocket-handler-list");
                    String[] tmpList = wsHandleList.split(",");
					for (int i = 0; i < tmpList.length; i++) {
						String[] item  =  tmpList[i].split(":");
						actionTypeList.add(new MsgActionType(item[0].trim(),"", item[1].trim()));
					}
				}
			}
		}
	}

	/**
	 * loadActionTypeInfoFromDb
	 * @param _type
	 * @return 如果不支持当前请求的动作，则返回null
	 */
	public static MsgActionType GetActionTypeByName(String _type) {
		if (StringUtils.isNullOrEmpty(_type)) {
			return null;
		}

		if (actionTypeList.size() == 0) {
			loadActionTypeInfoFromDb();
		}

		for (int i = 0; i <= actionTypeList.size() - 1; i++) {
			if (actionTypeList.get(i).getName().trim().equalsIgnoreCase(_type.trim())) {
				return actionTypeList.get(i);
			}
		}
		return null;
	}
}

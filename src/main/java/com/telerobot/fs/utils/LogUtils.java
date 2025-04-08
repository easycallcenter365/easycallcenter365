package com.telerobot.fs.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 日志工具类
 * 
 * @author: easycallcenter365@126.com
 *
 */
public class LogUtils {

	/** 系统换行符 */
	public static final String CRLF = System.getProperty("line.separator", "\n");

	public static final int LEVEL_DEBUG = 1;
	public static final int LEVEL_INFO = 2;
	public static final int LEVEL_WARN = 3;
	public static final int LEVEL_ERROR = 4;

	private ThreadLocal<List<ILog>> logBuf = new ThreadLocal<List<ILog>>();

	private static final Log logger = LogFactory.getLog(LogUtils.class);

	private static LogUtils instance = new LogUtils();

	private LogUtils() {

	}

	public static LogUtils getInstance() {
		return LogUtils.instance;
	}

	public void info(String msg) {
		getBuf().add(new ILog(LEVEL_INFO, msg));
	}

	public void debug(String msg) {
		getBuf().add(new ILog(LEVEL_DEBUG, msg));
	}

	public void warn(String msg) {
		getBuf().add(new ILog(LEVEL_WARN, msg));
	}

	public void error(String msg) {
		getBuf().add(new ILog(LEVEL_ERROR, msg));
	}

	public List<ILog> getBuf() {
		List<ILog> buf = logBuf.get();
		if (buf == null) {
			buf = new ArrayList<ILog>();
			logBuf.set(buf);
		}
		return buf;
	}

	public void log() {
		StringBuffer debug = new StringBuffer("");
		StringBuffer info = new StringBuffer("");
		StringBuffer warn = new StringBuffer("");
		StringBuffer error = new StringBuffer("");
		Iterator<ILog> it = getBuf().iterator();
		while (it.hasNext()) {
			ILog log = it.next();
			switch (log.getLevel()) {
			case LEVEL_DEBUG:
				debug.append(log.getMsg());
				debug.append(CRLF);
				break;
			case LEVEL_INFO:
				info.append(log.getMsg());
				info.append(CRLF);
				break;
			case LEVEL_WARN:
				warn.append(log.getMsg());
				warn.append(CRLF);
				break;
			case LEVEL_ERROR:
				error.append(log.getMsg());
				error.append(CRLF);
				break;
			}
		}
		String strDebug = debug.toString();
		String strInfo = info.toString();
		String strWarn = warn.toString();
		String strError = error.toString();
		if (StringUtils.isNotEmpty(strDebug)) {
			logger.debug(strDebug);
		}
		if (StringUtils.isNotEmpty(strInfo)) {
			logger.info(strInfo);
		}
		if (StringUtils.isNotEmpty(strWarn)) {
			logger.warn(strWarn);
		}
		if (StringUtils.isNotEmpty(strError)) {
			logger.error(strError);
		}
		getBuf().clear();
	}

	public class ILog {
		private int level;
		private String msg;

		public ILog(int level, String msg) {
			this.level = level;
			this.msg = msg;
		}

		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}

	}

}

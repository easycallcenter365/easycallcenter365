package com.telerobot.fs.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateUtils {

	public static final Date DEF_START = parseDate("1971-01-01");
	public static final Date DEF_END = parseDate("2037-01-01");


	/** 锁对象 */
	private static final Object lockObj = new Object();

	/** 存放不同的日期模板格式的sdf的Map */
	private static Map<String, ThreadLocal<SimpleDateFormat>> sdfMap = new HashMap<String, ThreadLocal<SimpleDateFormat>>();

	/**
	 * 返回一个ThreadLocal的sdf,每个线程只会new一次sdf
	 *
	 * @param pattern
	 * @return
	 */
	private static SimpleDateFormat getSdf(final String pattern) {
		ThreadLocal<SimpleDateFormat> tl = sdfMap.get(pattern);

		// 此处的双重判断和同步是为了防止sdfMap这个单例被多次put重复的sdf
		if (tl == null) {
			synchronized (lockObj) {
				tl = sdfMap.get(pattern);
				if (tl == null) {
					// 只有Map中还没有这个pattern的sdf才会生成新的sdf并放入map
					// 这里是关键,使用ThreadLocal<SimpleDateFormat>替代原来直接new SimpleDateFormat
					tl = new ThreadLocal<SimpleDateFormat>() {
						@Override
						protected SimpleDateFormat initialValue() {
							return new SimpleDateFormat(pattern);
						}
					};
					sdfMap.put(pattern, tl);
				}
			}
		}
		return tl.get();
	}

	public static void main(String[] args) throws InterruptedException {

	 Thread thread =	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(11000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String dateStr =	 getSdf("yyyy-MM-dd").format(new Date());
				System.out.println(dateStr);

				String dateStr2 =	 getSdf("yyyy-MM-dd").format(new Date());
				System.out.println(dateStr2);
			}
		});
	 thread.start();

    	String dateStr =	 getSdf("yyyy-MM-dd").format(new Date());
		System.out.println(dateStr);
		thread.join();

	}


	public static boolean isBetween(Date v, Date start, Date end) {
		if (v.compareTo(start) >= 0 && v.compareTo(end) <= 0) {
			return true;
		}
		return false;
	}

	public static boolean isConflict(Date v1, Date v2, Date v3, Date v4) {
		if (isBetween(v1, v3, v4)) {
			return true;
		}
		if (isBetween(v2, v3, v4)) {
			return true;
		}
		if (isBetween(v3, v1, v2)) {
			return true;
		}
		if (isBetween(v4, v1, v2)) {
			return true;
		}
		return false;
	}

	public static Date parse(String val, String format) {
		try {
			return getSdf(format).parse(val);
		} catch (Exception e) {
		}
		return null;
	}

	public static Date parseDate(String val) {
		return parse(val, "yyyy-MM-dd");
	}
	
	/**
	 * 获取当天的日期字符串; yyyy-MM-dd 格式;
	 * @return
	 */
	public static String getTodayDateStr(){
		SimpleDateFormat df = getSdf("yyyy-MM-dd");//设置日期格式
		return df.format(new Date()); 
	}
	
	public static Date parseDateTime(String val) {
		return parse(val, "yyyy-MM-dd HH:mm:ss");
	}
	
	public static Date addDays(Date val, int amount){
		return org.apache.commons.lang.time.DateUtils.addDays(val, amount);
	}
	
	public static Date addSeconds(Date val, int seconds)
	{
		return org.apache.commons.lang.time.DateUtils.addSeconds(val, seconds);
	}

	public static Date parse(String val, String format, Date def) {
		Date d = parse(val, format);
		if (d != null) {
			return d;
		}
		return def;
	}

	public static int GetYearFromDate(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}
	
	public static int GetMonthFromDate(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.MONTH) + 1;
	}
	
	public static int GetDayFromDate(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_MONTH);
	}
	
	public static String format(Date val, String format) {
		try {
			return getSdf(format).format(val);
		} catch (Exception e) {
		}
		return null;
	}

	public static String formatDate(Date val) {
		return format(val, "yyyy-MM-dd");
	}

	public static String formatDateTime(Date val) {
		return format(val, "yyyy-MM-dd HH:mm:ss");
	}

	public static String formatDateTimeEx(Date val) {
		return format(val, "yyyy-MM-dd HH:mm:ss SSS");
	}
	
	
	/**
	 * 初始化查询时间时间范围
	 * 
	 * @param startTime
	 * @param endTime
	 * @param defaultDelayTime
	 *            如果没有选择结束时间，默认向前推移的天数
	 * @return
	 * @throws ParseException
	 */
	public static List<String> getFormatTimePeriod(String startTime,
			String endTime, Integer defaultDelayTime) {
		List<String> titles = null;
		Calendar ss = Calendar.getInstance();
		ss.setTime(new Date());
		DateFormat format = getSdf("yyyy-MM-dd");
		try {
			if (endTime == null || "".equals(endTime)) {
				endTime = format.format(ss.getTime());
			} else {
				endTime = format.format(format.parse(endTime));
			}
			if (startTime == null || "".equals(startTime)) {
				ss.add(Calendar.DATE, -defaultDelayTime);
				startTime = format.format(ss.getTime());
			} else {
				startTime = format.format(format.parse(startTime));
			}

			titles = getPeriodDays(startTime, endTime, format);
		} catch (Exception e) {
			
		}

		return titles;
	}
	
	
	/**
	 * 获取一段时间，防止日志中缺少某天的数据
	 * 
	 * @param start
	 * @param end
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	@SuppressWarnings("unused")
	private static List<String> getPeriodDays(String start, String end,
			DateFormat format) throws ParseException {
		Date st = format.parse(start);
		Date en = format.parse(end);
		List<String> list = new ArrayList<String>();
		if (st.after(en))
			return null;
		Calendar ss = Calendar.getInstance();
		ss.setTime(st);
		Calendar ee = Calendar.getInstance();
		ee.setTime(en);
		do {
			list.add(format.format(ss.getTime()));
			ss.add(Calendar.DATE, 1);
		} while (ss.compareTo(ee) <= 0);
		return list;
	}
	
    /**  
     * 计算两个日期之间相差的天数    
     * @author: easycallcenter365@126.com
     * @param smdate 较小的时间 
     * @param bdate  较大的时间 
     * @return 相差天数  
     * @throws ParseException
     * @date: 2016年11月29日 上午10:40:46
     */
	public static int daysBetween(Date smdate, Date bdate) {
		if(null == smdate || null == bdate){
			return 0;
		}
		try {
			SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
			smdate = sdf.parse(sdf.format(smdate));
			bdate = sdf.parse(sdf.format(bdate));
			Calendar cal = Calendar.getInstance();
			cal.setTime(smdate);
			long time1 = cal.getTimeInMillis();
			cal.setTime(bdate);
			long time2 = cal.getTimeInMillis();
			long between_days = (time2 - time1) / (1000 * 3600 * 24);

			return Integer.parseInt(String.valueOf(between_days));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}  
      
    /**  
     * 计算两个时间之间相差的秒数    
     * @author: easycallcenter365@126.com
     * @param smdate 较小的时间 
     * @param bdate  较大的时间 
     * @return 相差天数  
     * @throws ParseException
     * @date: 2016年11月29日 上午10:40:46
     */
	public static int secondsBetween(Date smdate, Date bdate) {
		if(null == smdate || null == bdate){
			return 0;
		}
		try {
			SimpleDateFormat sdf = getSdf("yyyy-MM-dd HH:mm:ss");
			smdate = sdf.parse(sdf.format(smdate));
			bdate = sdf.parse(sdf.format(bdate));
			Calendar cal = Calendar.getInstance(); 
			cal.setTime(smdate);
			long time1 = cal.getTimeInMillis();
			cal.setTime(bdate);
			long time2 = cal.getTimeInMillis();
			long seconds = (time2 - time1) / (1000);

			return Integer.parseInt(String.valueOf(seconds));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}  
	
    /**  
     * 计算两个时间之间相差的秒数    
     * @author: easycallcenter365@126.com
     * @param smdate 较小的时间 
     * @param bdate  较大的时间 
     * @return 相差天数  
     * @throws ParseException
     * @date: 2016年11月29日 上午10:40:46
     */
	public static long secondsBetweenEx(Date smdate, Date bdate) {
		if(null == smdate || null == bdate){
			return 0L;
		}
		try {
			Calendar cal = Calendar.getInstance(); 
			cal.setTime(smdate);
			long time1 = cal.getTimeInMillis();
			cal.setTime(bdate);
			long time2 = cal.getTimeInMillis();
			long seconds = time2 - time1;

			return seconds;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0L;
		}
	} 
	
	/**
	 * 取后n秒的时间 
	 * @auth zlx
	 */
	/*public static Date addSeconds(Date date, int n) {
		try {
			Calendar ca = Calendar.getInstance();
			ca.setTime(date);
			ca.add(Calendar.SECOND, 1*n);
			return ca.getTime();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}*/
	
    /**
     * 计算两个日期之间相差的天数    字符串的日期格式的计算 
     * @author: easycallcenter365@126.com
     * @param smdate
     * @param bdate
     * @return
     * @throws ParseException
     * @date: 2016年11月29日 上午10:41:26
     */
	public static int daysBetween(String smdate, String bdate) {

		if(null == smdate || null == bdate){
			return 0;
		}
		
		try {
			SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.setTime(sdf.parse(smdate));
			long time1 = cal.getTimeInMillis();
			cal.setTime(sdf.parse(bdate));
			long time2 = cal.getTimeInMillis();
			long between_days = (time2 - time1) / (1000 * 3600 * 24);

			return Integer.parseInt(String.valueOf(between_days));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * 获取日期月份的最后一天
	 * @author: easycallcenter365@126.com
	 * @param d
	 * @return
	 * @date: 2016年12月12日 下午6:42:03
	 */
	public static String getLastDayOfMonthStr(Date d) {

		if(d==null){
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		
		//获取某月最大天数
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		//设置日历中月份的最大天数
		cal.set(Calendar.DAY_OF_MONTH, lastDay);
		
		//格式化日期
		SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
		String lastDayOfMonth = sdf.format(cal.getTime());
		
		return lastDayOfMonth;
	}
	
	/**
	 * 获取日期月份的最后一天
	 * @author: easycallcenter365@126.com
	 * @param d
	 * @return
	 * @date: 2016年12月12日 下午6:42:03
	 */
	public static Date getLastDayOfMonthDate(Date d) {
		
		if(d==null){
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		
		//获取某月最大天数
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		//设置日历中月份的最大天数
		cal.set(Calendar.DAY_OF_MONTH, lastDay);
		
		return cal.getTime();
	}
	
	/**
	 * 获取当月第一天
	 * @author: easycallcenter365@126.com
	 * @return
	 * @date: 2016年12月13日 下午1:42:12
	 */
	public static String getFirstDayStr(Date d) {
    	
        SimpleDateFormat df = getSdf("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        
        calendar.setTime(d);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String day_first = df.format(calendar.getTime());
        return day_first;

    }	
    
	/**
	 * 获取当月第一天
	 * @author: easycallcenter365@126.com
	 * @return
	 * @date: 2016年12月13日 下午1:42:12
	 */
	public static Date getFirstDayDate(Date d) {
    	
        Calendar calendar = Calendar.getInstance();
        
        calendar.setTime(d);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date day_first = calendar.getTime();
        return day_first;

    }
	
    /**
	 * 返回前N个月的日期值
	 * 
	 * @return
	 */
	public static Date getNextMonth(Integer m) {
		Calendar ca = Calendar.getInstance();
		ca.set(Calendar.DAY_OF_MONTH, 1);// 第一天
		if(m != 0) {
			ca.add(Calendar.MONTH, m);
		}
		return ca.getTime();
	}
	
	/**
	 * 返回前下个月的日期值
	 * 
	 * @param date
	 * @return
	 */
	public static Date getNextMonth(Date date) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.set(Calendar.DAY_OF_MONTH, 1);// 第一天
		ca.add(Calendar.MONTH, 1);
		return ca.getTime();
	}
	
	
	/**
	 * 返回下n个月账单日
	 * 
	 * @return
	 */
	public static Date getNextMonthPaymentDay(Date paymentDue,Integer m,Integer billDate) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(paymentDue);
		ca.set(Calendar.DAY_OF_MONTH, billDate);
		if(m != 0) {
			ca.add(Calendar.MONTH, m);
		}
		return ca.getTime();
	}
	
	/**
	 * 取后几个月
	 * @author: easycallcenter365@126.com
	 * @param date
	 * @return
	 * @date: 2016年12月22日 下午3:13:50
	 */
	public static Date addMonth(Date date, int n) {
		try {
			Calendar ca = Calendar.getInstance();
			ca.setTime(date);
			ca.add(Calendar.MONTH, 1*n);
			return ca.getTime();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}

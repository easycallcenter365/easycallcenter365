package com.telerobot.fs.utils;

public class ThreadUtil {

    public static void sleep(int mills)
    {
    	try {
			Thread.sleep(mills);
		} catch (InterruptedException e) {
		}
    }
    

    
    public static double getMemoryUsedEx()
    {
    	Runtime run = Runtime.getRuntime();
    	double total = run.totalMemory();
    	double free = run.freeMemory();
    	double used = (total - free)/(1024*1024);
    	return used;
    }
}

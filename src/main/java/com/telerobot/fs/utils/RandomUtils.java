package com.telerobot.fs.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomUtils {
//	public static void main(String[] args){
//		int[] a = getRandomSequence(10);
//		for(int i =0;i<a.length;i++)
//		System.out.println(a[i]);
//	}
	public static int getRandomNum(int total){
		Random random = new Random();
		return random.nextInt(total);
	}
	
	public static int getRandomByRange(int min ,int max){
		  if(min == max){
		   	 return min;
		  }
		  Random rand = new Random();
		  return rand.nextInt(max - min + 1) + min;
	}
	
	/**
	 * 
	 * @description 随机生成某一范围不重复的数字
	 * @author: easycallcenter365@126.com
	 * @date 2016年11月23日 下午2:06:45
	 * @param total
	 * @return
	 */
	public static int[] getRandomSequence(int total){
        int[] sequence = new int[total];
        int[] output = new int[total];
        for (int i = 0; i < total; i++)
            sequence[i] = i;
        Random random = new Random();
        int end = total - 1;
        for (int i = 0; i < total; i++){
            int num = random.nextInt(end+1);
            output[i] = sequence[num];
            sequence[num] = sequence[end];
            end--;
        }
        return output;
	}
	
	/**
	 * 
	 * @description 随机生成某一范围不重复的数字
	 * @author: easycallcenter365@126.com
	 * @date 2016年11月23日 下午2:06:45
	 * @param total
	 * @return
	 */
	public static List<Integer> getListRandomSequence(int total){
        int[] sequence = new int[total];
       List<Integer> output = new ArrayList<Integer>();
        for (int i = 0; i < total; i++)
            sequence[i] = i;
        Random random = new Random();
        int end = total - 1;
        for (int i = 0; i < total; i++){
            int num = random.nextInt(end+1);
             output.add(sequence[num]);
            sequence[num] = sequence[end];
            end--;
        }
        return output;
	}


}

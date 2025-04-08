package com.telerobot.fs.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.core.io.Resource;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class FileUtils {

	/**
	 * 合并返回的录音文件为1个
	 * @param ttsFile
	 * @param files
	 * @return
	 * @auth zlx
	 * @date 2020年1月2日 下午16:42
	 */
	public static boolean combineListFiles(String ttsFile, String files) {
		String[] fileArrs = files.split(";");
		String hellowordPath = "/data/robot/voice/HelloWorld.wav";
		File hellowordFile = new File(hellowordPath);
		File firstFile = new File(fileArrs[0]);
		try {
			if (hellowordFile.exists()) {
				org.apache.commons.io.FileUtils.copyFile(hellowordFile, new File(ttsFile));
			} else {
				org.apache.commons.io.FileUtils.copyFile(firstFile, new File(ttsFile));
			}
		} catch (IOException e) {
		   return false;
		}
		File out = new File(ttsFile);
		if(!out.exists()){
			out.mkdirs();
		}
		try {
			AudioFileFormat aff = null;
			if (fileArrs.length > 0) {
				aff = AudioSystem.getAudioFileFormat(firstFile);
				ArrayList<AudioInputStream> aisList = new ArrayList<AudioInputStream>();
				long frameLenght = 0L;
				for (String file : fileArrs) {
					if(StringUtils.isNotBlank(file)){
						File f = new File(file);
						int waitTtsTime = 0;
						while (waitTtsTime < 10 && !f.exists()) {
							ThreadUtil.sleep(100);
							waitTtsTime ++;
						}
						AudioInputStream ais = AudioSystem.getAudioInputStream(f);
						aisList.add(ais);
						frameLenght += ais.getFrameLength();
					}
				}
				Enumeration<AudioInputStream> en = Collections.enumeration(aisList);
				SequenceInputStream sis = new SequenceInputStream(en);
				AudioSystem.write(new AudioInputStream(sis, aff.getFormat(), frameLenght), aff.getType(), out);
				if (sis != null) {
					sis.close();
				}
				return true;
			}

		} catch (Exception e) {
		}
		return false;
	}

	public static String ReadFile(String filePath, String encoding) {
		String content = "";
		if(!new File(filePath).exists()) return "";
		Resource res1 = new FileSystemResource(filePath); 
		EncodedResource encRes = new EncodedResource(res1,encoding);
		try {
			content = FileCopyUtils.copyToString(encRes.getReader());
		} catch (Exception e) {
			System.out.println("error reading file: " + filePath  + e.toString());
		}
		return content;
	}
	
	public static boolean WriteFile(String filePath, String content)
	{
		try
		{
		FileWriter fw = new FileWriter(filePath);
		PrintWriter pw = new PrintWriter(fw);
		pw.println(content);
		pw.flush();
		pw.close();
		}
		catch(Throwable ex)
		{
			return false;
		}
		return true;
	}
}

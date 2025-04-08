package com.telerobot.fs.utils;

import java.io.*;

/**
 * 文件操作工具类
 */
public class FileUtil {
	public static boolean writeToLocal(String path, byte[] data) throws IOException {
		File file = new File(path);
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream fop = new FileOutputStream(file);
		if (!file.exists()) {
			file.createNewFile();
		}
		fop.write(data);
		fop.flush();
		fop.close();
		return true;
	}
	/**
	 * 读取文件内容为二进制数组
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] read2ByteArray(String filePath) throws IOException {

		InputStream in = new FileInputStream(filePath);
		byte[] data = inputStream2ByteArray(in);
		in.close();

		return data;
	}

	/**
	 * 流转二进制数组
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] inputStream2ByteArray(InputStream in) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024 * 4];
		int n = 0;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}
	
	public static void WriteStringToFile(String filePath, String content) {  
        try {  
            PrintWriter pw = new PrintWriter(new FileWriter(filePath));  
            pw.println(content);    
            pw.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
	
	  public static void copyFile(File fromFile,File toFile) throws IOException{
	        FileInputStream ins = new FileInputStream(fromFile);
	        FileOutputStream out = new FileOutputStream(toFile);
	        byte[] b = new byte[1024];
	        int n=0;
	        while((n=ins.read(b))!=-1){
	            out.write(b, 0, n);
	        }
	        
	        ins.close();
	        out.close();
	    }
	  
	  public static byte[] readFile(String path)  {
	        File f = new File(path);
	        if(!f.exists()){
	            try {
	                throw new FileNotFoundException(path);
	            } catch (FileNotFoundException e) {
	                e.printStackTrace();
	            }
	        }
	        try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
				ByteArrayOutputStream bos = new ByteArrayOutputStream((int)f.length())){
	            int buf_size = 9999;
	            byte[] buffer = new byte[buf_size];
	            int len = 0;
	            while(-1 != (len = in.read(buffer,0,buf_size))){
	                bos.write(buffer,0,len);
	            }
	            return bos.toByteArray();
	        }catch (IOException e) {
	            e.printStackTrace();
	        }
	        return null;
	    }
	  
	public static boolean delFile(File file) {
	        if (!file.exists()) {
	            return false;
	        }
	        if (file.isDirectory()) {
	            File[] files = file.listFiles();
	            for (File f : files) {
	                delFile(f);
	            }
	        }
	        return file.delete();
	    }
}

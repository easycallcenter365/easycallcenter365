package com.telerobot.fs.config;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;

public class CallConfig {

	/**
	 *  录音路径（存储路径，可能是docker内部的路径）
	 */
	public static final String RECORDINGS_PATH = SystemConfig.getValue("recording_path");

}

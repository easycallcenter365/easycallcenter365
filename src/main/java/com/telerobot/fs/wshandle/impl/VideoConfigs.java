package com.telerobot.fs.wshandle.impl;

import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.utils.StringUtils;
import com.telerobot.fs.wshandle.MessageResponse;
import com.telerobot.fs.wshandle.PhoneCallType;
import com.telerobot.fs.wshandle.RespStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VideoConfigs {
    protected static final Logger logger = LoggerFactory.getLogger(VideoConfigs.class);

    public static final String DEFAULT_VIDEO_LEVEL = "42E01E";

    /**
     * 视频清晰度列表
     */
    private static List<String> videoLevels = new ArrayList<String>(10);

    /**
     *  视频会议布局layouts
     */
    private static List<String> conferenceLayouts = new ArrayList<String>(5);


    /**
     * 视频会议模版
     */
    private static List<String> conferenceVideoTemplates = new ArrayList<>(10);

    /**
     * 检查视频会议模版
     * @return
     */
    public static boolean checkConferenceVideoTemplate(String value){
        return  conferenceVideoTemplates.contains(value);
    }

    /**
     * 检查视频会议布局layouts
     * @return
     */
    public static boolean checkConferenceLayouts(String value){
        return  conferenceLayouts.contains(value);
    }


    /**
     *  判断给定的videoLevel是否符合需求;
     * @param videoLevel
     * @return
     */
    public static boolean checkVideoLevels(String videoLevel){
        return  VideoConfigs.videoLevels.contains(videoLevel);
    }


    /**
     * 检查视频会议参数是否合法
     * @return
     */
    public static MessageResponse checkVideoConferenceParameters(String currentVideoLayOut, String currentCallType, String currentConfTemplate){
        if(currentVideoLayOut == null || currentVideoLayOut.trim().length() == 0
                || !VideoConfigs.checkConferenceLayouts(currentVideoLayOut) ){
            return new MessageResponse(RespStatus.REQUEST_PARAM_ERROR, "conference layout parameter missing or invalid!");
        }

        if(currentCallType == null || currentCallType.trim().length() == 0
                || !PhoneCallType.checkCallTypeValid(currentCallType)){
            return new MessageResponse(RespStatus.REQUEST_PARAM_ERROR, "conference callType parameter missing or invalid!");
        }

        if(currentConfTemplate == null || currentConfTemplate.trim().length() == 0
                || !VideoConfigs.checkConferenceVideoTemplate(currentConfTemplate)){
            return new MessageResponse(RespStatus.REQUEST_PARAM_ERROR, "conference confTemplate parameter missing or invalid!");
        }
        return  null;
    }

    static {
        videoLevels.add(DEFAULT_VIDEO_LEVEL);
        String videoLevelList = SystemConfig.getValue("video_level_id_list");
        logger.info("当前可用视频清晰度列表：{}", videoLevelList);
        String[] _array = videoLevelList.split(",");
        for (String item : _array) {
            if(!videoLevels.contains(item.trim())) {
                if(!StringUtils.isNullOrEmpty(item.trim())) {
                    videoLevels.add(item.trim());
                }
            }
        }

        String layouts = SystemConfig.getValue("conference_video_layouts");
        logger.info("当前可用会议布局layouts有：{}", layouts);
        _array = layouts.split(",");
        for (String item : _array) {
            if(!StringUtils.isNullOrEmpty(item.trim())) {
                conferenceLayouts.add(item.trim());
            }
        }

        String videoTemplates = SystemConfig.getValue("conference_video_templates");
        logger.info("当前可用会议模版有：{}", videoTemplates);
        _array = videoTemplates.split(",");
        for (String item : _array) {
            if(!StringUtils.isNullOrEmpty(item.trim())) {
                conferenceVideoTemplates.add(item.trim());
            }
        }
    }
}

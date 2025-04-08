package com.telerobot.fs.wshandle;

public class PhoneCallType {

    public  static  final String AUDIO_CALL = "audio";

    public  static  final String VIDEO_CALL = "video";

    public static boolean checkVideoCall(String callType){
        return VIDEO_CALL.equalsIgnoreCase(callType);
    }

    public static boolean checkAudioCall(String callType){
        return AUDIO_CALL.equalsIgnoreCase(callType);
    }

    public static boolean checkCallTypeValid(String callType){
        return VIDEO_CALL.equalsIgnoreCase(callType) || AUDIO_CALL.equalsIgnoreCase(callType);
    }
}

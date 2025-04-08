package com.telerobot.fs.entity.bo;


public class RobotInteractiveParam {

	private volatile boolean inSpeaking = false;

	private volatile  int inHangUpState = 0;

	private volatile String dtmfDigit;

	private volatile int allowInterrupt = 0;

	public boolean checkInSpeaking() {
		return inSpeaking;
	}

	public void setInSpeaking(boolean inSpeaking) {
		this.inSpeaking = inSpeaking;
	}

	public String getDtmfDigit() {
		return dtmfDigit;
	}

	public void setDtmfDigit(String dtmfDigit) {
		this.dtmfDigit = dtmfDigit;
	}

	public boolean checkInHangupState() {
		return inHangUpState == 1;
	}

	public void setInHangUpState(boolean inHangup) {
		if(inHangup) {
			inHangUpState = 1;
		}
	}

	public int getAllowInterrupt(){
		return  allowInterrupt;
	}

	public void setAllowInterrupt(int allowInterrupt) {
		this.allowInterrupt = allowInterrupt;
	}
}
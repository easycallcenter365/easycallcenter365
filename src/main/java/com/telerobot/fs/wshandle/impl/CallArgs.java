package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSONObject;

public class CallArgs{
     private  String cmd;
     private JSONObject args;
	 public String getCmd() {
		 return cmd;
	 }
	 public void setCmd(String cmd) {
		 this.cmd = cmd;
	 }
	 public JSONObject getArgs() {
		 return args;
	 }
	 public void setArgs(JSONObject args) {
		 this.args = args;
	 }
 }
package com.telerobot.fs.entity.bo;

public class ConfMember
    {
	    public ConfMember(){}
        
        public ConfMember(String realName, String telephone)
        {
            this.name = realName;
            this.phone = telephone;
        }

        private String name = "";

	    private String roomNo = "";

        private String phone = "";

		private String videoLevel = "";

		private String callType = "";

        private long addTime = System.currentTimeMillis();

        private Long answeredTime = 0L;

        private Long hangupTime = 0L;

		/**
		 * 挂机原因
		 */
		private String hangupClause = "";

		/**
		 * 挂机代码
		 */
		private String sipCode = "";

        private String callUuid = "";

        private String conferenceMemberId = "";

		/**
		 * 通话状态
		 */
		private ConfMemerStatus status = ConfMemerStatus.waitToCall;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public long getAddTime() {
			return addTime;
		}

		public void setAddTime(long addTime) {
			this.addTime = addTime;
		}

		public Long getAnsweredTime() {
			return answeredTime;
		}

		public void setAnsweredTime(Long answeredTime) {
			this.answeredTime = answeredTime;
		}

		public Long getHangupTime() {
			return hangupTime;
		}

		public void setHangupTime(Long hangupTime) {
			this.hangupTime = hangupTime;
		}

		public String getHangupClause() {
			return hangupClause;
		}

		public void setHangupClause(String hangupClause) {
			this.hangupClause = hangupClause;
		}

		public String getCallUuid() {
			return callUuid;
		}

		public void setCallUuid(String callUuid) {
			this.callUuid = callUuid;
		}

		public String getConferenceMemberId() {
			return conferenceMemberId;
		}

		public void setConferenceMemberId(String conferenceMemberId) {
			this.conferenceMemberId = conferenceMemberId;
		}

		public ConfMemerStatus getStatus() {
			return status;
		}

		public void setStatus(ConfMemerStatus status) {
			this.status = status;
		}

		public String getVideoLevel() {
			return videoLevel;
		}

		public void setVideoLevel(String videoLevel) {
			this.videoLevel = videoLevel;
		}

		public String getCallType() {
			return callType;
		}

		public void setCallType(String callType) {
			this.callType = callType;
		}

		public String getSipCode() {
			return sipCode;
		}

		public void setSipCode(String sipCode) {
			this.sipCode = sipCode;
		}

		public String getRoomNo() {
			return roomNo;
		}

		public void setRoomNo(String roomNo) {
			this.roomNo = roomNo;
		}
	}

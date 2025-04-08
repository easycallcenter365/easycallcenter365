package com.telerobot.fs.wshandle;


    public class MsgActionType
    {
        private String name;
        private String description;
        private String classFullName;
        public MsgActionType() { }

        public MsgActionType(String _para_Name, String _para_Description, String _para_Dllfile)
        {
            this.name = _para_Name;
            this.description = _para_Description;
            this.classFullName = "com.telerobot.fs.wshandle.impl." + _para_Dllfile;
        }

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getClassFullName() {
			return classFullName;
		}

		public void setClassFullName(String classFullName) {
			this.classFullName = classFullName;
		}
    } 

 
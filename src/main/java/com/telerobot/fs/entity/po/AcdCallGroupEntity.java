package com.telerobot.fs.entity.po;


   /***
    * acd_call_group 实体类
    ***/ 
/**
 * ACD业务组信息
 *
 */
public class AcdCallGroupEntity {

	public  AcdCallGroupEntity()  {}

	public AcdCallGroupEntity(int group_id, int press_key, String description) {
		super();
		this.group_id = group_id;
		this.press_key = press_key;
		this.description = description;
	}

	private int group_id;
	private int press_key;
	private String description="";

	/**  业务组编号  **/
	public void setGroup_id(int group_id){
		this.group_id=group_id;
	}

	/**  业务组编号  **/
	public int getGroup_id(){
		return group_id;
	}

	/**  坐席按键  **/
	public void setPress_key(int press_key){
		this.press_key=press_key;
	}

	/**  坐席按键  **/
	public int getPress_key(){
		return press_key;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AcdCallGroupEntity [group_id=" + group_id + ", press_key=" + press_key + ", description=" + description
				+ "]";
	}


}


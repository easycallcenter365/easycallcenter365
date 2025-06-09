package com.telerobot.fs.entity.pojo;


/**
 * 座席状态枚举
 * @author easycallcenter365@gmail.com
 */
public enum AgentStatus {
    /**
     * 空闲
     */
    free("free", 1),
    /**
     * 通话中;
     */
    calling("calling", 2),


    /**
     * 事后处理中
     */
    busy("busy", 3),
    /**
     * 休息中
     */
    rest("rest", 4),
    /**
     * 培训中
     */
    train("train", 5),
    /**
     * 会议中
     */
    meeting("meeting", 6),
    /**
     *  刚登录系统
     */
    justLogin("justLogin", 7);

    /**
     *  状态描述
     */
    private String name;
    /**
     * index
     */
    private int index;

    public  static AgentStatus getItemByValue(int index){
        AgentStatus[] items = AgentStatus.values();
        for(AgentStatus item : items){
            if(item.getIndex() == index){
                return item;
            }
        }
        return null;
    };

    private AgentStatus(String name, int index) {
        this.name = name;
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", this.index,  this.name);
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

} 

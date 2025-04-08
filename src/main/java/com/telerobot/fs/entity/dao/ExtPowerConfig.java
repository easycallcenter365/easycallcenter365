package com.telerobot.fs.entity.dao;

/**
 *  分机权限配置信息对象;
 */
public class ExtPowerConfig {
    private int powerId;
    private String extNum;
    private String groupId;

    public int getPowerId() {
        return powerId;
    }

    public void setPowerId(int powerId) {
        this.powerId = powerId;
    }

    public String getExtNum() {
        return extNum;
    }

    public void setExtNum(String extNum) {
        this.extNum = extNum;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}

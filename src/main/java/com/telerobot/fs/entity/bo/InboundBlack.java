package com.telerobot.fs.entity.bo;

public class InboundBlack {
    private int id;
    private String caller;
    private long expiredTime;
    private long addTime;
    private String addUser;

    // Constructors, getters, and setters

    public InboundBlack() {
    }

    public InboundBlack(int id, String caller, long expiredTime, long addTime, String addUser) {
        this.id = id;
        this.caller = caller;
        this.expiredTime = expiredTime;
        this.addTime = addTime;
        this.addUser = addUser;
    }

    // Getters and setters for each field

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public String getAddUser() {
        return addUser;
    }

    public void setAddUser(String addUser) {
        this.addUser = addUser;
    }
}

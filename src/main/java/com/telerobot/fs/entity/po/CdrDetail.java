package com.telerobot.fs.entity.po;

public class CdrDetail {

    private String uuid;

    /**
     *  话单类型： inbound、outbound
     */
    private String cdrType;

    /**
     * 话单消息体;
     */
    private String cdrBody;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCdrType() {
        return cdrType;
    }

    public void setCdrType(String cdrType) {
        this.cdrType = cdrType;
    }

    public String getCdrBody() {
        return cdrBody;
    }

    public void setCdrBody(String cdrBody) {
        this.cdrBody = cdrBody;
    }
}

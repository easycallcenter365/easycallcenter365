package com.telerobot.fs.entity.bo;

public enum ChannelFlag {

    /**
     * 当前通话已经被重邀请视频通话
     */
    RE_INVITE_VIDEO("RE_INVITE_VIDEO", 1),


    /**
     * 保持通话
     */
    HOLD_CALL("HOLD_CALL", 2),



    /**
     * 转接的通话
     */
    TRANSFER_CALL_RECV("TRANSFER_CALL_RECV", 3),

    /**
     *  当前通话已收到振铃媒体
     */
    RECV_RING_MEDIA ("RECV_RING_MEDIA", 999);


    /**
     *  状态描述
     */
    private String name;
    /**
     * index
     */
    private int index;

    public  static ChannelFlag getItemByIndex(int index){
        ChannelFlag[] items = ChannelFlag.values();
        for(ChannelFlag item : items){
            if(item.getIndex() == index){
                return item;
            }
        }
        return null;
    };

    private ChannelFlag(String name, int index) {
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

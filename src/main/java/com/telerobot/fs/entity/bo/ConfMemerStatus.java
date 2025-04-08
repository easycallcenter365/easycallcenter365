package com.telerobot.fs.entity.bo;


public enum ConfMemerStatus {

    /**
     * 待呼叫
     */
    waitToCall("waitToCall", 1),
    /**
     * 呼叫中;
     */
    calling("calling", 2),
    /**
     * 通话中
     */
    connected("connected", 3),

    /**
     * 已挂机
     */
    hangup("hangup", 4);


    /**
     *  状态描述
     */
    private String name;
    /**
     * index
     */
    private int index;

    public  static ConfMemerStatus getItemByValue(int index){
        ConfMemerStatus[] items = ConfMemerStatus.values();
        for(ConfMemerStatus item : items){
            if(item.getIndex() == index){
                return item;
            }
        }
        return null;
    };

    private ConfMemerStatus(String name, int index) {
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

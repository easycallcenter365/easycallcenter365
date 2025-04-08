package com.telerobot.fs.entity.bo;

public enum ChanneState {

    /**
     * 刚创建
     */
    INIT ("INIT", 0),

    /**
     *  通道已接通
     */
    ANSWERED ("ANSWERED", 1),


    /**
     * 通话已经Park
     */
    PARKED ("PARKED", 2),


    /**
     * 桥接
     */
    BRIDGED ("BRIDGED", 3),

    /**
     * 通话已挂机
     */
    HANGUP ("HANGUP", 4);


    /**
     *  状态描述
     */
    private String name;
    /**
     * index
     */
    private int index;

    public  static ChanneState getItemByIndex(int index){
        ChanneState[] items = ChanneState.values();
        for(ChanneState item : items){
            if(item.getIndex() == index){
                return item;
            }
        }
        return null;
    };

    private ChanneState(String name, int index) {
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

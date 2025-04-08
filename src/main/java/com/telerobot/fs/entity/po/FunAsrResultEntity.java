package com.telerobot.fs.entity.po;

public class FunAsrResultEntity {

   private boolean final_flag;
   private String vad_type;
   private String text;

    public boolean isFinal_flag() {
        return final_flag;
    }

    public void setFinal_flag(boolean final_flag) {
        this.final_flag = final_flag;
    }

    public String getVad_type() {
        return vad_type;
    }

    public void setVad_type(String vad_type) {
        this.vad_type = vad_type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

package com.blink.framework.validate.constant;

public enum DictType {

    STRING("S"),DECIMAL("D"),NUMBER("N");

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    DictType(String type){
        this.type = type;
    }
}

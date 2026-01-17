package com.blink.framework.mq.constant;

public enum ExchangeType {

    FANOUT("fanout"),DIRECT("direct"), TOPIC("topic"),HEADERS("headers");

    ExchangeType(String s){
        this.name = s;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.fangcloud.phoenix.common.protocol;

public enum Protocol {
    HESSIAN("hessian"),
    DUBBO("dubbo");

    private String value;

    Protocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

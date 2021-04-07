package com.xiaoju.framework.handler;

/**
 * Created by didi on 2021/3/22.
 */
public enum CaseWsMessages {
    CLIENT_CLOSE(0, "agiletc is closing"),
    PONG(1, "pong pong pong"),
    PING(2, "ping ping ping"),
    UNDEFINED(3, "undefined"),
    LOCK(4, "lock"),
    UNLOCK(5, "unlock");

    private Integer value;
    private String msg;

    CaseWsMessages(Integer value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getValue() {
        return value;
    }
}

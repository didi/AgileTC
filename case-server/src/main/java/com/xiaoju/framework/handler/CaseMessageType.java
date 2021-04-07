package com.xiaoju.framework.handler;

/**
 * Created by didi on 2021/4/4.
 */
public enum CaseMessageType {
    PING('0'),
    EDITOR('1'),
    CTRL('2'),
    NOTIFY('3');

    private final char flag;
    CaseMessageType(char flag) {
        this.flag = flag;
    }

}

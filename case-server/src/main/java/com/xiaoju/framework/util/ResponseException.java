package com.xiaoju.framework.util;

/**
 * Created by didi on 2019/9/24.
 */
public class ResponseException extends RuntimeException {
    private Integer errorCode;
    private String msg;

    public ResponseException() {
    }

    public ResponseException(ErrorCode error, String errorMsg) {
        super(errorMsg);
        this.errorCode = Integer.valueOf(error.getCode());
        this.msg = errorMsg;
    }

    public ResponseException(ErrorCode error) {
        this.errorCode = Integer.valueOf(error.getCode());
        this.msg = error.getMsg();
    }

    public ResponseException(Integer errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
        this.msg = msg;
    }

    public Integer getErrorCode() {
        return this.errorCode;
    }

    public String getMsg() {
        return this.msg;
    }

}

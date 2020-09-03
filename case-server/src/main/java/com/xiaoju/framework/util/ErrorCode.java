package com.xiaoju.framework.util;

/**
 * Created by didi on 2019/9/24.
 */
public enum ErrorCode {
    SUCCESS(200, "success"),
    UN_LOGIN(401, "未登录"),
    UN_AUTHORIZED(403, "无此权限"),
    COMMON_LOCK_ERROR(10100, "服务器繁忙，请稍后再试"),
    COMMON_UNLOCK_ERROR(10101, "服务器繁忙，请稍后再试"),
    COMMON_PARAM_ERROR(10401, "服务器繁忙，请稍后再试"),
    COMMON_NO_PARAM(10400, "必填参数未填写"),
    VERSION_NOT_MATCH(10500,"暂不支持xmind zen版本，请上传xmind 8版本文件");

    private int code;
    private String msg;

    private ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

    public String getMsg(String msg) {
        return msg.length() == 0 ? this.msg : this.msg + ": " + msg;
    }
}

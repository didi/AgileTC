package com.xiaoju.framework.util;

import org.springframework.ext.common.object.Status;

/**
 * Created by didi on 2019/9/29.
 */
public enum StatusCode implements Status{
    JSON_FORMAT_ERROR(10004, "JSON格式不正确"),
    DATA_FORMAT_ERROR(10005, "数据格式化异常"),
    HTTP_ACESS_ERROR(10006, "HTTP访问异常"),
    FILE_EMPTY_ERROR(10007, "上传失败，请选择文件"),
    FILE_FORMATE_ERROR(10008, "文件格式不对，请上传xmind文件"),
    FILE_IMPORT_ERROR(10009, "导入失败，请稍后再试"),
    SERVICE_RUN_SUCCESS(10000, "服务运行成功"),
    RECORD_UPDATE_ERROR(10000, "服务运行成功"),
    ;


    private int status;
    private String msg;

    StatusCode(int status, String message) {
        this.status = status;
        this.msg = message;
    }

    @Override
    public boolean isSuccess() {
        return getStatus() == 10000;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMsg() {
        return String.format(msg, "");
    }

    @Override
    public String getMsg(Object... objects) {
        if (objects == null) {
            return getMsg();
        }

        return String.format(msg, objects);
    }
}

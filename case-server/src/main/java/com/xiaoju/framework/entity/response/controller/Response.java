package com.xiaoju.framework.entity.response.controller;

import com.xiaoju.framework.constants.enums.StatusCode;
import lombok.Data;

/**
 * controller层统一响应体
 *
 * @author didi
 * @date 2020/9/24
 */
@Data
public class Response<T> {
    private Integer code;
    private String msg;
    private T data;

    public Response() {
        this.code = StatusCode.SERVICE_RUN_SUCCESS.getStatus();
    }

    public static <T> Response<T> build(int status, String msg) {
        return build(status, msg, null);
    }

    public static <T> Response<T> build(Status status, String msg) {
        return build(status.getStatus(), msg, null);
    }

    public static <T> Response<T> build(Status status) {
        return build(status.getStatus(), status.getMsg(), null);
    }

    public static <T> Response<T> build(int status, String msg, T data) {
        Response<T> response = new Response<>();
        response.setCode(status);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

    public static <T> Response<T> success() {
        return build(StatusCode.SERVICE_RUN_SUCCESS);
    }

    public static <T> Response<T> success(T data) {
        return build(StatusCode.SERVICE_RUN_SUCCESS.getStatus(), StatusCode.SERVICE_RUN_SUCCESS.getMsg(), data);
    }
}

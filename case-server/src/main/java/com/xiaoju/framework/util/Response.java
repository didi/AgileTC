package com.xiaoju.framework.util;

/**
 * Created by didi on 2019/9/24.
 */
public class Response<T> {
    private Integer code;
    private String msg;
    private T data;

    public Response() {
        this.code = Integer.valueOf(ErrorCode.SUCCESS.getCode());
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCode() {
        return this.code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static <T> Response<T> build(ErrorCode code, T data) {
        return build(code.getCode(), code.getMsg(), data);
    }

    public static <T> Response<T> build(ErrorCode code) {
        return build(code.getCode(), code.getMsg(), null);
    }

    public static <T> Response<T> build(ErrorCode code, String msg) {
        return build(code.getCode(), msg, null);
    }

    public static <T> Response<T> build(ErrorCode code, String msg, T data) {
        return build(code.getCode(), msg, data);
    }

    public static <T> Response<T> build(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> Response<T> build(int code, String msg, T data) {
        Response response = new Response();
        response.setCode(Integer.valueOf(code));
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

    public static <T> Response<T> success() {
        return build(ErrorCode.SUCCESS);
    }

    public static <T> Response<T> success(T data) {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    public static void main(String[] args) {
        System.out.println(Response.success());
    }
}

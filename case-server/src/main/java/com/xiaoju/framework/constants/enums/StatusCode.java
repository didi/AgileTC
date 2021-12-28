package com.xiaoju.framework.constants.enums;


import com.xiaoju.framework.entity.response.controller.Status;

/**
 * 自定义前后端传输编码
 *
 * @author didi
 * @date 2020/9/29
 */
public enum StatusCode implements Status {
    // 枚举
    // 这里的200不建议改，如果需要改，前端组件判断success的判断也需要更改，单纯兼容，如果有代码洁癖可以改为10000
    SERVICE_RUN_SUCCESS(200, "服务运行成功"),
    JSON_FORMAT_ERROR(10004, "JSON格式不正确"),
    DATA_FORMAT_ERROR(10005, "数据格式化异常"),
    HTTP_ACCESS_ERROR(10006, "HTTP访问异常"),
    FILE_FORMAT_ERROR(10007, "文件格式不对，请上传xmind文件"),
    FILE_IMPORT_ERROR(10008, "导入失败，请稍后再试"),
    FILE_EXPORT_ERROR(10009, "导出失败，请稍后再试"),
    EXCEL_FORMAT_ERROR(10010, "EXCEL格式不匹配"),
    NODE_ALREADY_EXISTS(20001, "节点已存在"),
    WS_UNKNOWN_ERROR(100010, "websocket访问异常"),
    AUTHORITY_ERROR(100011, "权限认证错误"),
    ASPECT_ERROR(100012, "权限内部处理错误"),

    // 内部异常
    INTERNAL_ERROR(10400, "内部参数校验或逻辑出错"),
    VERSION_NOT_MATCH(10500,"暂不支持xmind zen版本，请上传xmind 8版本文件"),
    NOT_FOUND_ENTITY(10600, "没有该项数据"),

    // 统一异常
    SERVER_BUSY_ERROR(99999, "服务器正忙，请稍后再试");

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

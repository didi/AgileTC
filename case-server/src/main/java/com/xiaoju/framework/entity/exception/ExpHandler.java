package com.xiaoju.framework.entity.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.response.controller.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常捕捉器
 *
 * @author didi
 * @date 2020/11/20
 */
@ControllerAdvice
@ResponseBody
public class ExpHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExpHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<?> handlerException(IllegalArgumentException e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.INTERNAL_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.INTERNAL_ERROR, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response<?> handlerException(MissingServletRequestParameterException e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.INTERNAL_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.INTERNAL_ERROR, e.getMessage());
    }

    @ExceptionHandler(JsonMappingException.class)
    public Response<?> handlerException(JsonMappingException e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.JSON_FORMAT_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.JSON_FORMAT_ERROR);
    }

    @ExceptionHandler(JsonParseException.class)
    public Response<?> handlerException(JsonParseException e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.DATA_FORMAT_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.DATA_FORMAT_ERROR);
    }

    /**
     * 自定义异常
     */
    @ExceptionHandler(CaseServerException.class)
    public Response<?> handlerException(CaseServerException e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.INTERNAL_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.INTERNAL_ERROR, e.getMessage());
    }

    /**
     * 这里的全局异常是捕捉controller下没有try catch Exception的Method的
     */
    @ExceptionHandler(Exception.class)
    public Response<?> handlerException(Exception e) {
        e.printStackTrace();
        LOGGER.error(StatusCode.SERVER_BUSY_ERROR.getCode(), e.getMessage());
        return Response.build(StatusCode.SERVER_BUSY_ERROR);
    }
}

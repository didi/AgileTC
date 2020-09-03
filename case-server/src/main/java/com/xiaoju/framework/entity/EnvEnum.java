package com.xiaoju.framework.entity;

import com.xiaoju.framework.util.ErrorCode;
import com.xiaoju.framework.util.ResponseException;

import java.util.Arrays;

/**
 * Created by didi on 2019/9/30.
 */
public enum EnvEnum {
    TestEnv(0, "测试环境"),
    PreEnv(1, "预发环境"),
    OnlineEnv(2, "线上环境"),
    TestQaEnv(3, "冒烟qa"),
    TestRdEnv(4, "冒烟rd"),
    SourceEnv(10, "原始用例模式"),
    SmkEnv(20, "冒烟用例"),
    NaotuEnv(100, "脑图通用场景用法");

    private Integer value;
    private String name;

    EnvEnum(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static EnvEnum transfer(Integer value) {
        try {
            return Arrays.stream(EnvEnum.values()).filter(e -> e.getValue().equals(value)).findFirst().get();
        } catch (Exception e) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM,"枚举值报错");
        }
    }
}

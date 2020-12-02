package com.xiaoju.framework.constants.enums;

/**
 * 执行任务环境枚举
 *
 * @author didi
 * @date 2020/9/24
 */
public enum EnvEnum {
    // 枚举
    TestEnv(0, "测试环境"),
    PreEnv(1, "预发环境"),
    OnlineEnv(2, "线上环境"),
    TestQaEnv(3, "冒烟qa"),
    TestRdEnv(4, "冒烟rd");

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
}

package com.xiaoju.framework.constants.enums;

/**
 * 圈选用例枚举
 *
 * @author didi
 * @date 2020/3/19
 */
public enum PriorityEnum {
    // 枚举
    Priority0(1,"P0用例"),
    Priority1(2,"P1用例"),
    Priority2(3,"P2用例");

    private Integer value;
    private String name;

    PriorityEnum(Integer value, String name){
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

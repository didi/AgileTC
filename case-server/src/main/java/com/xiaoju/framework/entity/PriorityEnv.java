package com.xiaoju.framework.entity;

/**
 * @author jiangxia
 * @date 2020-03-19
 * @description
 */
public enum PriorityEnv {
    Priority0(1,"P0用例"),
    Priority1(2,"P1用例"),
    Priority2(3,"P2用例");

    private Integer value;
    private String name;

    PriorityEnv(Integer value,String name){
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

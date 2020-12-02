package com.xiaoju.framework.entity.xmind;

/**
 * 数字封装
 *
 * @author didi
 * @date 2019/10/10
 */
public class IntCount {
    int value;
    public IntCount(int value) {
        this.value = value;
    }
    public void add() {
        this.value ++;
    }
    public int get() {
        return this.value;
    }
    public void del() {
        this.value --;
    }
    public void set(int value) {
        this.value = value;
    }
}

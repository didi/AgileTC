package com.xiaoju.framework.constants.enums;

/**
 * 脑图中，执行任务的执行结果枚举类
 *
 * @author didi
 * @date 2020/8/13
 */
public enum ProgressEnum {
    // 枚举类
    FAIL(1),
    IGNORE(4),
    BLOCK(5),
    SUCCESS(9),
    DEFAULT(0)
    ;

    private Integer progress;

    ProgressEnum(Integer progress) {
        this.progress = progress;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public static ProgressEnum findEnumByProgress(Integer progress) {
        for (ProgressEnum progressEnum : ProgressEnum.values()) {
            if (progressEnum.getProgress().equals(progress)) {
                return progressEnum;
            }
        }
        return DEFAULT;
    }
}
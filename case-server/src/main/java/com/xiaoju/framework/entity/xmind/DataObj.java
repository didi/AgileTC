package com.xiaoju.framework.entity.xmind;

import lombok.Data;

/**
 * 任务的json单元
 *
 * @author didi
 * @date 2020/8/13
 */
@Data
public class DataObj {

    /**
     * 用例行为 阻塞、失败、成功、不执行
     */
    private Integer progress;

    /**
     * 用例标题
     */
    private String text;

    /**
     * 节点id
     */
    private String id;

    public String getProgressStr() {
        return String.valueOf(progress);
    }
}

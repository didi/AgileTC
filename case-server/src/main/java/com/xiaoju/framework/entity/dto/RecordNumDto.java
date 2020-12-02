package com.xiaoju.framework.entity.dto;

import lombok.Data;


/**
 * 执行任务数量转换体
 *
 * @author didi
 * @date 2020/6/9
 */
@Data
public class RecordNumDto {

    /**
     * 所属用例id
     */
    Long caseId;

    /**
     * 任务数量
     */
    Integer recordNum;

}

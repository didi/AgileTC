package com.xiaoju.framework.entity.response.records;

import lombok.Data;

import java.util.Date;

/**
 * 脑图 - 执行任务上方的概览信息
 *
 * @author hcy
 * @date 2020/10/28
 */
@Data
public class RecordGeneralInfoResp {

    private Long id;

    private Long caseId;

    private String title;

    /**
     * 预计周期
     */
    private Date expectStartTime;

    private Date expectEndTime;

    /**
     * 需求id
     */
    private String requirementIds;

    /**
     * 用例执行数
     */
    private int passCount;

    /**
     * 用例总数
     */
    private int totalCount;

    /**
     * 用例通过数
     */
    private int successCount;

    /**
     * 用例失败数
     */
    private int bugNum;

    /**
     * 用例阻塞数
     */
    private int blockCount;

    /**
     * 忽略个数
     */
    private int ignoreCount;

    /**
     * 通过率
     */
    private double passRate;
}

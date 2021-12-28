package com.xiaoju.framework.entity.response.records;

import lombok.Data;

import java.util.Date;

/**
 * 任务列表查询体
 *
 * @author didi
 * @date 2020/10/27
 */
@Data
public class RecordListResp {

    private Long id;

    private String title;

    private String creator;

    /**
     * 任务id，该字段做保留
     */
    private Long recordId;

    /**
     * 用例id
     */
    private Long caseId;

    /**
     * 责任人
     */
    private String owner;

    /**
     * 执行人列表  以逗号分隔
     */
    private String executors;

    /**
     * 失败数
     */
    private Integer bugNum;

    /**
     * 阻塞数
     */
    private Integer blockNum;

    /**
     * 成功数
     */
    private Integer successNum;

    /**
     * 执行总数=失败+阻塞+成功
     */
    private Integer executeNum;

    /**
     * 用例总数，progress=4不会计入
     */
    private Integer totalNum;

    /**
     * 圈选用例内容
     */
    private String chooseContent;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 计划周期-开始时间
     */
    private Date expectStartTime;

    /**
     * 计划周期-结束时间
     */
    private Date expectEndTime;

    /**
     * 用例描述
     */
    private String description;

}

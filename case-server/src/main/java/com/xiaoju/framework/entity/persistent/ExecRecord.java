package com.xiaoju.framework.entity.persistent;

import lombok.Data;

import java.util.Date;

/**
 * 执行记录
 *
 * @author didi
 * @date 2019/09/29
 */
@Data
public class ExecRecord {

    private Long id;

    private Long caseId;

    private String title;

    /**
     * 默认0,环境
     */
    @Deprecated
    private Integer env;

    /**
     * 执行任务的执行记录
     */
    private String caseContent;

    private Integer isDelete;

    /**
     * 用例执行数
     */
    private Integer passCount;

    /**
     * 用例总数
     */
    private Integer totalCount;

    private String creator;

    private String modifier;

    private Date gmtCreated;

    private Date gmtModified;

    /**
     * 用例通过数
     */
    private Integer successCount;

    /**
     * 用例忽略数 -- 不需要执行 -- 也不计算在内
     */
    private Integer ignoreCount;

    /**
     * 用例阻塞数
     */
    private Integer blockCount;

    /**
     * 用例失败数
     */
    private Integer failCount;

    /**
     * 执行人
     */
    private String executors;

    /**
     * 描述
     */
    private String description;

    /**
     * 圈选用例选项
     * {"priority":[1,2,3]}
     * priority：0-所有用例，1-p0用例，2-p1用例，3-p2用例
     */
    private String chooseContent;

    /**
     * 计划开始时间
     */
    private Date expectStartTime;

    /**
     * 计划结束时间
     */
    private Date expectEndTime;

    /**
     * 实际开始时间
     */
    @Deprecated
    private Date actualStartTime;

    /**
     * 实际解决时间
     */
    @Deprecated
    private Date actualEndTime;

    /**
     * 负责人
     */
    private String owner;

}

package com.xiaoju.framework.entity.persistent;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用例
 *
 * @author didi
 * @date 2019/7/12
 */
@Data
public class TestCase {
    private Long id;

    private Long groupId;

    private String title;

    private String description;

    private Integer isDelete;

    private String creator;

    private String modifier;

    private Date gmtCreated;

    private Date gmtModified;

    private String extra;

    private Long productLineId;

    private Integer caseType;

    private String caseContent;

    /**
     * 模块id 已经废弃
     */
    @Deprecated
    private Long moduleNodeId;

    private String requirementId ;

    /**
     * 冒烟用例id，目前冒烟用例已经集成到执行任务中，废弃
     */
    @Deprecated
    private Long smkCaseId;



    private Integer channel;

    private String bizId;
}
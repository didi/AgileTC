package com.xiaoju.framework.entity.persistent;

import lombok.Data;

import java.util.Date;

/**
 * 备份
 *
 * @author didi
 * @date 2019/11/05
 */
@Data
public class CaseBackup {
    private Long id;
    private Long caseId;
    private String title;
    private String creator;
    private Date gmtCreated;
    private String caseContent;
    private String recordContent;
    private String extra;
    private Integer isDelete;
    /**
     * 本次变更应用的 patch 内容。如果是冲突副本，则此处记录的是冲突无法应用的 patch 内容
     */
    private String jsonPatch;

    /**
     * 是否为保存冲突时记录的副本
     */
    private Boolean isConflict;
}

package com.xiaoju.framework.entity;

import java.util.Date;

/**
 * Created by didi on 2019/11/5.
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getModifier() {
        return creator;
    }

    public void setModifier(String modifier) {
        this.creator = modifier == null ? null : modifier.trim();
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra == null ? null : extra.trim();
    }

    public String getCaseContent() {
        return caseContent;
    }

    public void setCaseContent(String caseContent) {
        this.caseContent = caseContent == null ? null : caseContent.trim();
    }

    public String getRecordContent() {
        return recordContent;
    }

    public void setRecordContent(String caseContent) {
        this.recordContent = caseContent == null ? null : caseContent.trim();
    }
    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
}

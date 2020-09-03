package com.xiaoju.framework.entity;

import java.util.Date;
import java.util.List;

public class TestCase {
    private Long id;

    private Long groupId;

    private String title;

    private String description;

    private Integer isDelete; /* 0-否；1-是 */

    private String creator;

    private String modifier;

    private Date gmtCreated;

    private Date gmtModified;

    private String extra;

    private Long productLineId;

    private Integer caseType;

    private Long moduleNodeId;

    private String requirementId ;

    private Long smkCaseId;

    private String caseContent;

    private Integer channel;

    private Integer totalCount;

    private Integer RecordNum;

    private List<ExecRecord> execRecordList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier == null ? null : modifier.trim();
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra == null ? null : extra.trim();
    }

    public Long getProductLineId() {
        return productLineId;
    }

    public void setProductLineId(Long productLineId) {
        this.productLineId = productLineId;
    }

    public Integer getCaseType() {
        return caseType;
    }

    public void setCaseType(Integer caseType) {
        this.caseType = caseType;
    }

    public Long getModuleNodeId() {
        return moduleNodeId;
    }

    public void setModuleNodeId(Long moduleNodeId) {
        this.moduleNodeId = moduleNodeId;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public Long getSmkCaseId() {
        return smkCaseId;
    }

    public void setSmkCaseId(Long smkCaseId) {
        this.smkCaseId = smkCaseId;
    }

    public String getCaseContent() {
        return caseContent;
    }

    public void setCaseContent(String caseContent) {
        this.caseContent = caseContent == null ? null : caseContent.trim();
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRecordNum() {
        return RecordNum;
    }

    public void setRecordNum(Integer recordNum) {
        RecordNum = recordNum;
    }

    public List<ExecRecord> getExecRecordList() {
        return execRecordList;
    }

    public void setExecRecordList(List<ExecRecord> execRecordList) {
        this.execRecordList = execRecordList;
    }

    public String toString() {
        return "id: " + id + " \ngroupId:" + groupId + " \ntitle:" + title + " \ndesc:" + description + " \ncreator:" + creator
                + " \nmodifier:" + modifier + " \ngmtCreated:" + gmtCreated  + " \ngmtModified:" + gmtModified + " \nextra:" + extra
                + " \nproductLineId:" + productLineId + " \ncaseType:" + caseType + " \nmoduleNodeId:"+ moduleNodeId
                + "\nrequirementIds:" + requirementId + "\nsmkCaseId:" + smkCaseId + "\ncaseContent:" + caseContent+"\nchannel"+channel;
    }
}
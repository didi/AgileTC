package com.xiaoju.framework.entity.request.cases;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用例 筛选与查询
 *
 * @author hcy
 * @date 2020/8/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseQueryReq {

    private Long id;

    private Integer caseType;

    private Long lineId;

    private String title;

    private String creator;

    private String requirementId;

    private String beginTime;

    private String endTime;

    private Integer channel;

    private String bizId;

    private Integer pageNum;

    private Integer pageSize;

    public CaseQueryReq(Integer caseType, String title, String creator, String reqIds, String beginTime, String endTime, Integer channel, String bizId, Long lineId, Integer pageNum, Integer pageSize) {
        this.caseType = caseType;
        this.title = title;
        this.creator = creator;
        this.requirementId = reqIds;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.channel = channel;
        this.bizId = bizId;
        this.lineId = lineId;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCaseType() {
        return caseType;
    }

    public void setCaseType(Integer caseType) {
        this.caseType = caseType;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

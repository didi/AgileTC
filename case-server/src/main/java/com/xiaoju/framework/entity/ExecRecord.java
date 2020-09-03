package com.xiaoju.framework.entity;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * Created by didi on 2019/9/29.
 */
public class ExecRecord {
    private Long id;

    private String title;

    private Long caseId;

    /*{ id: 0, val: '测试环境' },
    { id: 1, val: '预发环境' },
    { id: 2, val: '线上环境' },
    { id: 3, val: '冒烟case-qa' },
    { id: 4, val: '冒烟case-rd' },
    { id: 10, val: '冒烟case' }*/
    private Integer env;

    private Integer isDelete;

    private String creator;

    private String caseContent;

    private String modifier;

    private Date gmtCreated;

    private Date gmtModified;

    private int passCount;

    private int totalCount;

    private int successCount;//成功数量

    private int bugNum;//bug数量,失败数量

    private int blockCount = 0;//阻塞数量

    private double progressRate;

    private double passRate;//通过率

    private String description;

    /**
     * 圈选用例选项
     * {"priority":[1,2,3]}
     * priority：0-所有用例，1-p0用例，2-p1用例，3-p2用例
     */
    private String chooseContent;

    //执行人
    private String executors;

    //预计周期
    private Date expectStartTime;

    private Date expectEndTime;

    //实际周期
    private Date actualStartTime;

    private Date actualEndTime;

    //负责人
    private String owner;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

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

    public Integer getEnv() {
        return env;
    }

    public void setEnv(Integer env) {
        this.env = env;
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

    public String getCaseContent() {
        return caseContent;
    }

    public void setCaseContent(String caseContent) {
        this.caseContent = caseContent;
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

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public Double getProgressRate() {
        return progressRate;
    }

    public void setProgressRate(Double progressRate) {
        this.progressRate = progressRate;
    }

    public Double getPassRate() {
        return passRate;
    }

    public void setPassRate(Double passRate) {
        this.passRate = passRate;
    }

    public String getExecutors() {
        return executors;
    }

    public void setExecutors(String executors) {
        this.executors = executors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChooseContent() {
        return chooseContent;
    }

    public int getBugNum() {
        return bugNum;
    }

    public void setBugNum(int bugNum) {
        this.bugNum = bugNum;
    }

    public void setChooseContent(String chooseContent) {
        this.chooseContent = chooseContent;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public Date getExpectStartTime() {
        return expectStartTime;
    }

    public void setExpectStartTime(Date expectStartTime) {
        this.expectStartTime = expectStartTime;
    }

    public Date getExpectEndTime() {
        return expectEndTime;
    }

    public void setExpectEndTime(Date expectEndTime) {
        this.expectEndTime = expectEndTime;
    }

    public Date getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(Date actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public Date getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(Date actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public String toString() {
        return "id: " + id + " \ntitle:" + title + " \ncaseId:" + caseId + " \ncreator:" + creator
                + " \nmodifier:" + modifier + " \ngmtCreated:" + gmtCreated  + " \ngmtModified:" + gmtModified
                + " \nenv:" + env + " \npassCount:" + passCount + " \ntotalCount:"+ totalCount
                + "\nprogressRate:" + progressRate + " \nsuccessCount:"+ successCount + "\ncaseContent:" + caseContent+"\ndescription:"+description
                +"\nexecutors:"+executors+"\nchooseContent"+chooseContent;
    }
}

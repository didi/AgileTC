package com.xiaoju.framework.entity.request.cases;

import lombok.Data;

/**
 * 用例 筛选与查询
 *
 * @author hcy
 * @date 2020/8/12
 */
@Data
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

    private String caseKeyWords;

    private Integer pageNum;

    private Integer pageSize;

    public CaseQueryReq(Integer caseType, String title, String creator, String reqIds, String beginTime, String endTime, Integer channel, String bizId, Long lineId, String caseKeyWords, Integer pageNum, Integer pageSize) {
        this.caseType = caseType;
        this.title = title;
        this.creator = creator;
        this.requirementId = reqIds;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.channel = channel;
        this.bizId = bizId;
        this.lineId = lineId;
        this.caseKeyWords = caseKeyWords;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}

package com.xiaoju.framework.entity.Response;

import java.util.List;

public class TestCasePriorityInfoResp {

    //p0,p1,p2用例等级对应的用例数
    private List<Integer> levelCaseNum;

    //用例总数
    private Integer TotalCount;

    public List<Integer> getLevelCaseNum() {
        return levelCaseNum;
    }

    public void setLevelCaseNum(List<Integer> levelCaseNum) {
        this.levelCaseNum = levelCaseNum;
    }

    public Integer getTotalCount() {
        return TotalCount;
    }

    public void setTotalCount(Integer totalCount) {
        TotalCount = totalCount;
    }
}

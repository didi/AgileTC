package com.xiaoju.framework.service;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.Response.ConditionTestCaseInfoVO;
import com.xiaoju.framework.entity.Response.TestCasePriorityInfoResp;
import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.util.PageResult;
import com.xiaoju.framework.util.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by didi on 2019/9/23.
 */
public interface WebSocketService {
    public void createTestCase(TestCase testCase);
    public String selectCaseById(String caseId);
    public TestCase selectByPrimaryKey(Long id);
    public int update(TestCase testCase);
    public int delete(String caseId);
    public List<TestCase> getIdByRequirementId(Long requirementId);
    public PageResult<TestCase> listTestCase(Long productLineId,
                                             Integer case_type,
                                             Integer pageNum,
                                             Integer pageSize,
                                             Long id,
                                             String title,
                                             String creator,
                                             String requirement_id,
                                             String beginTime,
                                             String endTime,
                                             Integer channel);
    public TestCase clearTestCaseById(Long id);
    public TestCase clearTestCase(TestCase testCaseObj);
    public TestCase getJsonByXmind(String file,String creator,Long productLine_id,String requirementId,String title,String description,Integer channel);
    public TestCase getJsonByXmindJson(String file,String creator,Long productLine_id,String requirementId,String title,String description,Integer channel);
    public HttpServletResponse getXmindById(Long id, HttpServletResponse response, HttpServletRequest request);
    public List<Map<String,Object>> listCreators(Integer case_type, Long productLineId);
    public TestCasePriorityInfoResp getPriorityInfoByCaseId(Long caseId);
    ConditionTestCaseInfoVO getCountByCondition(JSONObject jsonObject);
    public int caseAddRequirementId(TestCase testCase);
}

package com.xiaoju.framework.controller;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.*;
import com.xiaoju.framework.entity.Response.ConditionTestCaseInfoVO;
import com.xiaoju.framework.entity.Response.TestCasePriorityInfoResp;
import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.service.ExecRecordService;
import com.xiaoju.framework.service.WebSocketService;
import com.xiaoju.framework.util.ErrorCode;
import com.xiaoju.framework.util.PageResult;
import com.xiaoju.framework.util.Response;
import com.xiaoju.framework.util.TreeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by didi on 2019/9/23.
 */
@Api(description = "case接口")
@RestController
@Component
@RequestMapping("/api/case")
@Slf4j
public class WebSocketController {
    @Autowired
    WebSocketService webSocketService;
    @Autowired
    ExecRecordService execRecordService;
    @Autowired
    CaseBackupService caseBackupService;
    @Autowired
    WebSocket webSocket;
    @Autowired
    TestCaseMapper testCaseMapper;
    @Autowired
    private HttpServletRequest request;

    @ApiOperation("创建case用例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "productLineId", value = "业务线id", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "title", value = "case名称", required = true, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "caseType", value = "需求类型", required = true, paramType = "query", dataType = "int")

    })
    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<Long> createTestCase(@RequestBody TestCase testCase) {
        if (testCase == null || StringUtils.isEmpty(testCase.getTitle())) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "请填写用例名称");
        }
        String username = request.getHeader("oe-username");
        if (username != null) {
            testCase.setCreator(username);
        }
        webSocketService.createTestCase(testCase);
        return Response.success(testCase.getId());
    }

    @ApiOperation("物理删除case用例")
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteTestCase(@RequestBody TestCase testCase) {
        Long id = testCase.getId();
        // TODO: 该接口是硬删除，需要修改
        int ret = webSocketService.delete(String.valueOf(id));
        return String.valueOf(ret);
    }

    @ApiOperation("逻辑删除case用例")
    @RequestMapping(value = "/softdelete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<?> deleteTestCaseSoft(@RequestBody TestCase testCase) {
        Long id = testCase.getId();
        testCase.setIsDelete(1);
        int ret = webSocketService.update(testCase);
        return Response.success();
    }

    @ApiOperation("根据需求获取测试用例的id")
    @ApiImplicitParam(name = "requirementId", value = "需求id", required = true, paramType = "query", dataType = "long")
    @RequestMapping(value = "/getCaseByRequirementId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<List<TestCase>> getIdByRequirementId(@RequestParam(value = "requirementId") Long requirementId
    ) {
        List testCaseList = webSocketService.getIdByRequirementId(requirementId);
        return Response.success(testCaseList);
    }

    @ApiOperation(value = "根据业务线id，分页获取case列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<PageResult<TestCase>> listTestCase(@RequestParam(value = "productLineId", required = false) Long productLineId,
                                                       @RequestParam(value = "case_type") Integer case_type,
                                                       @RequestParam(value = "id", required = false) Long id,
                                                       @RequestParam(value = "title", required = false) String title,
                                                       @RequestParam(value = "creator", required = false) String creator,
                                                       @RequestParam(value = "requirement_id", required = false) String requirement_id,
                                                       @RequestParam(value = "beginTime", required = false) String beginTime,
                                                       @RequestParam(value = "endTime", required = false) String endTime,
                                                       @RequestParam(value = "channel", required = false, defaultValue = "0") Integer channel,
                                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        PageResult<TestCase> pageModule = webSocketService.listTestCase(productLineId, case_type, pageNum, pageSize, id, title, creator, requirement_id, beginTime, endTime, channel);
        return Response.success(pageModule);
    }

    // 该接口暂时无人调用
    @ApiOperation("根据id获取case详情")
    @RequestMapping(value = "/getCaseById", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<TestCase> getRequirementList(@RequestParam(value = "id") Long id) {
        TestCase testCase = webSocketService.selectByPrimaryKey(id);
        testCase.setCaseContent("");
        return Response.success(testCase);
    }

    @ApiOperation("修改case用例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "caseId", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "title", value = "case名称", required = true, paramType = "query", dataType = "string")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<TestCase> updateTestCase(@RequestBody TestCase testCaseObject) {
        if (testCaseObject == null || StringUtils.isEmpty(testCaseObject.getTitle())) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "请填写用例名称");
        }
        if (testCaseObject == null || testCaseObject.getId() == null)
            return Response.build(ErrorCode.COMMON_NO_PARAM, "请指定要修改的caseId");

        String username = request.getHeader("oe-username");
        if (username != null) {
            testCaseObject.setModifier(username);
        }
        try {
            if (StringUtils.isEmpty(testCaseObject.getCaseContent())) { // 内容为空，修改用例标题，走老逻辑
                log.info("case title will be modified.");
                webSocketService.update(testCaseObject);
            } else { // 内容不为空，用户主动保存
                log.info("case content will be manual modified.");
                String caseContent = testCaseObject.getCaseContent();
                log.info("update case content is: \n" + caseContent);
                JSONObject jsonObject = TreeUtil.parse(caseContent);
                Long recordId = testCaseObject.getGroupId();// TODO: groupid暂时用于recordID

                if (recordId != null) { // 修改的是记录时，只保存记录不保存case
                    log.info(recordId.toString() + "save record exec, will not save case content");
                    if (jsonObject.containsKey("progress")) { // 打开的是记录模式
                        ExecRecord recordUpdate = new ExecRecord();
                        recordUpdate.setId(recordId);

                        //记录所有执行者
                        ExecRecord orgRecord = execRecordService.getRecordById(recordId);
                        StringBuilder executors = new StringBuilder();

                        if (orgRecord.getExecutors() == null || orgRecord.getExecutors().equals("")) {
                            executors = new StringBuilder(testCaseObject.getModifier());
                        } else {
                            String executor = orgRecord.getExecutors();
                            executors = new StringBuilder(executor);
                            String[] list = executor.split(",");
                            if (!Arrays.asList(list).contains(testCaseObject.getModifier())) {
                                //无重复则添加，又重复不添加
                                if (list.length == 0) {
                                    executors.append(testCaseObject.getModifier());
                                } else {
                                    executors.append("," + testCaseObject.getModifier());
                                }
                            }
                        }

                        recordUpdate.setExecutors(executors.toString());
                        Date date = new Date(System.currentTimeMillis());
                        recordUpdate.setGmtModified(date);
                        recordUpdate.setCaseContent(jsonObject.getString("progress"));
                        recordUpdate.setPassCount(jsonObject.getInteger("passCount"));
                        recordUpdate.setTotalCount(jsonObject.getInteger("totalCount"));
                        recordUpdate.setSuccessCount(jsonObject.getInteger("successCount"));
                        log.info("save record:" + recordId + " content:" + recordUpdate.toString().substring(0, 20));
                        execRecordService.modifyTestRecord(recordUpdate);
                    }

                } else {
                    log.info("prepare to save case content");
                    TestCase caseUpdate = new TestCase();
                    caseUpdate.setCaseContent(jsonObject.getString("content"));
                    caseUpdate.setId(testCaseObject.getId());
                    webSocketService.update(caseUpdate);
                }
                CaseBackup caseBackup = new CaseBackup();
                caseBackup.setCaseId(testCaseObject.getId());
                caseBackup.setTitle(testCaseObject.getTitle());
                caseBackup.setCaseContent(jsonObject.getString("content"));
                caseBackup.setRecordContent(jsonObject.getString("progress"));
                caseBackup.setModifier(testCaseObject.getModifier());
                caseBackup.setIsDelete(0);
                caseBackupService.insertBackup(caseBackup);
                String newRecordId = (recordId == null) ? "undefined" : recordId.toString();
                webSocket.updateSocketContent(testCaseObject.getId().toString(), newRecordId, caseContent);
                log.info("case backup success.id:" + testCaseObject.getId() + " recordId:" + newRecordId);
            }

            return Response.success();
        } catch (Exception e) {
            log.error("update test case failed. e: " + e.getMessage());
            return Response.build(ErrorCode.COMMON_PARAM_ERROR, "服务器异常，请联系开发人员");
        }
    }

    @ApiOperation(value = "获取所有需求用例创建者")
    @RequestMapping(value = "/listCreators", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<List<Map<String, Object>>> listCreators(@RequestParam(value = "case_type") Integer case_type, @RequestParam(value = "productLineId") Long productLineId) {
        if (case_type == null) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "用例类型不能为空");
        }
        if (case_type == null) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "业务线不能为空");
        }

        List<Map<String, Object>> result = webSocketService.listCreators(case_type, productLineId);
        return Response.success(result);
    }

    @ApiOperation(value = "获取用例的各level用例数")
    @RequestMapping(value = "/getPriorityInfoByCaseId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<TestCasePriorityInfoResp> getPriorityInfoByCaseId(@RequestParam(value = "caseId") Long caseId) {
        if (caseId == null) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "用例id不能为空");
        }
        return Response.success(webSocketService.getPriorityInfoByCaseId(caseId));
    }

    @ApiOperation(value = "根据筛选条件获取用例数")
    @PostMapping(value = "/countByCondition", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<ConditionTestCaseInfoVO> getCountByCondition(@RequestBody JSONObject jsonObject) {

        return Response.success(webSocketService.getCountByCondition(jsonObject));
    }

    @ApiOperation(value = "用例增加需求id")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseId", value = "caseid", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "requirementId", value = "需求id", required = true, paramType = "query", dataType = "long")
    })
    @RequestMapping(value = "/caseAddRequirementId", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<?> caseAddRequirementId(@RequestBody TestCase testCase) {
        if (testCase.getId() == null) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "用例id不能为空");
        }
        if (testCase.getRequirementId() == null) {
            return Response.build(ErrorCode.COMMON_NO_PARAM, "需求id不能为空");
        }
        return Response.success(webSocketService.caseAddRequirementId(testCase));
    }

    // 获取数量
    @ApiOperation("获取测试用例数量")
    @RequestMapping(value = "/getCount", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<Long> getCount(@RequestParam(value = "productLineId", required = false) Long productLineId,
                                   @RequestParam(value = "id", required = false) Long id,
                                   @RequestParam(value = "creator", required = false) String creator,
                                   @RequestParam(value = "requirement_id", required = false) String requirement_id,
                                   @RequestParam(value = "beginTime", required = false) String beginTime,
                                   @RequestParam(value = "endTime", required = false) String endTime,
                                   @RequestParam(value = "channel", required = false, defaultValue = "0") Integer channel) {
        Long sum = 0L;
        Integer pageNum = 1;

        Date sTime = null, eTime = null;
        if (beginTime != "" && beginTime != null) {
            SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                sTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(beginTime);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        if (endTime != "" && endTime != null) {
            SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                eTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endTime);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        List<TestCase> result = new ArrayList<TestCase>();
        List<TestCase> caseList = new ArrayList<TestCase>();
        List<Long> productLineList = new ArrayList<>();
        if (productLineId != null) {
            productLineList.add(productLineId);
        } else {
            productLineList = testCaseMapper.listProductLineId();
        }

        for (Long lineId : productLineList) {
            do {
                result = testCaseMapper.listTestCaseByids(lineId, 0, pageNum, 500, id, null, creator, null, sTime, eTime, channel);
                if (result != null) {
                    caseList.addAll(result);
                }
                pageNum++;
            } while (result.size() == 500);
        }

        Long productCount = 0L;
        for (TestCase tCase : caseList) {

            Long count = 0L;
            String caseContent = webSocketService.selectCaseById(tCase.getId().toString());
            if (caseContent != null && !caseContent.equals("")) {
                JSONObject caseData = TreeUtil.parse(caseContent);
                count = Long.parseLong(caseData.get("totalCount").toString());
            }
            productCount = productCount + count;
        }
        sum = sum + productCount;
        return Response.success(sum);
    }
}

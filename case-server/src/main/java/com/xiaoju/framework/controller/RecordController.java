package com.xiaoju.framework.controller;

import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.service.ExecRecordService;
import com.xiaoju.framework.util.ErrorCode;
import com.xiaoju.framework.util.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.xiaoju.framework.util.Response;
import com.xiaoju.framework.entity.ExecRecord;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by didi on 2019/9/29.
 */
@Api(description = "execRecord接口")
@Slf4j
@RestController
@RequestMapping(value = "/api/execRecord")
@EnableAsync
public class RecordController {
    @Autowired
    ExecRecordService execRecordService;
    @Autowired
    private HttpServletRequest request;

    @ApiOperation("查询执行记录列表")
    @ApiImplicitParam(name = "caseId", value = "caseId", required = true,paramType = "query",dataType = "long")
    @RequestMapping(value = "/getRecordByCaseId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<List<ExecRecord>> getRecordByCaseId(@RequestParam(value = "caseId") Long caseId) {
        List<ExecRecord> execRecordList =execRecordService.getRecordByCaseId(caseId);
        return Response.success(execRecordList);
    }

    @ApiOperation("根据需求id查询执行记录列表")
    @ApiImplicitParam(name = "requirementId", value = "requirementId", required = true,paramType = "query",dataType = "long")
    @RequestMapping(value = "/getRecordByRequirementId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<PageResult<ExecRecord>> getRecordByRequirementId(@RequestParam(value = "requirementId",required = true) Long requirementId,
                                                         @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize",defaultValue = "20") Integer pageSize,
                                                         @RequestParam(value = "channel",required = false,defaultValue = "0") Integer channel){
        PageResult<ExecRecord> PageExecRecordList =execRecordService.getRecordByRequirementId(requirementId,pageNum,pageSize,channel);

        return Response.success(PageExecRecordList);
    }

    @ApiOperation("查询执行记录的内容")
    @ApiImplicitParam(name = "id", value = "id", required = true,paramType = "query",dataType = "long")
    @RequestMapping(value = "/getContentById", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<ExecRecord> getRecordContentById(@RequestParam(value = "id") Long id) {
        ExecRecord execRecord =execRecordService.getRecordById(id);
        return Response.success(execRecord);
    }

    @ApiOperation("查询当前新增记录可用的环境")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseId", value = "caseId", required = true,paramType = "query",dataType = "long"),
            @ApiImplicitParam(name = "creator", value = "creator", required = true,paramType = "query",dataType = "string")
    })
    @RequestMapping(value = "/getEnv", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<List<Integer>> getEnv(@RequestParam(value = "caseId") Long caseId,@RequestParam(value = "creator") String creator) {
        List<Integer> envList =execRecordService.getEnv(caseId,creator);
        return Response.success(envList);
    }

    @ApiOperation("新增执行记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseId", value = "caseId", required = true,paramType = "query",dataType = "long"),
            @ApiImplicitParam(name = "creator", value = "creator", required = true,paramType = "query",dataType = "string"),
            //@ApiImplicitParam(name = "env", value = "env", required = true,paramType = "query",dataType = "int")
            //圈选记录条件不能为空
            //@ApiImplicitParam(name = "chooseContent", value = "chooseContent", required = true,paramType = "query",dataType = "string")
    })
    @RequestMapping(value = "/addRecord", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<Long> addRecord(@RequestBody ExecRecord execRecord) {
        String username = request.getHeader("oe-username");
        if(username != null) {
            execRecord.setCreator(username);
        }
        execRecord =  execRecordService.addRecord(execRecord);
        return Response.success(execRecord.getId());
    }

    @ApiOperation("修改执行记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "recordId", required = true,paramType = "query",dataType = "long"),
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<ExecRecord> updateTestCase(@RequestBody ExecRecord execRecord) {
        if (execRecord == null || execRecord.getId() == null)
            return Response.build(ErrorCode.COMMON_NO_PARAM, "请指定要修改的记录");
        execRecord = execRecordService.modifyTestRecord(execRecord);
        return Response.success(execRecord);

    }

    @ApiOperation("清除执行记录执行结果")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "recordId", required = true,paramType = "query",dataType = "long"),
    })
    @RequestMapping(value = "/clearResult", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<ExecRecord> clearRecord(@RequestBody ExecRecord execRecord) {
        if (execRecord == null || execRecord.getId() == null)
            return Response.build(ErrorCode.COMMON_NO_PARAM, "请指定要修改的记录");

        String username = request.getHeader("oe-username");
        if(username != null) {
            execRecord.setModifier(username);
        }

        execRecord = execRecordService.clearRecord(execRecord);
        return Response.success(execRecord);

    }

    @ApiOperation("逻辑删除记录")
    @RequestMapping(value = "/softdelete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<?> deleteRecordSoft(@RequestBody ExecRecord execRecord) {
        if (execRecord == null || execRecord.getId()==null){
            return Response.build(ErrorCode.COMMON_NO_PARAM, "删除记录id不能为空");
        }
        int ret = execRecordService.deleteRecordById(execRecord.getId());
        return Response.success();
    }

    @ApiOperation("编辑记录信息")
    @RequestMapping(value = "/EditRecord", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<?> editRecord(@RequestBody ExecRecord execRecord){
        if (execRecord == null || execRecord.getId()==null){
            return Response.build(ErrorCode.COMMON_NO_PARAM, "记录id不能为空");
        }
        String username = request.getHeader("oe-username");
        if(username != null) {
            execRecord.setModifier(username);
        }
        if (execRecord == null || execRecord.getModifier()==null){
            return Response.build(ErrorCode.COMMON_NO_PARAM, "修改者不能为空");
        }
        if (execRecord == null || execRecord.getTitle()==null){
            return Response.build(ErrorCode.COMMON_NO_PARAM, "记录名称不能为空");
        }
        if (execRecord == null || execRecord.getChooseContent()==null){
            return Response.build(ErrorCode.COMMON_NO_PARAM, "圈选内容不能为空");
        }
        execRecordService.editRecord(execRecord);
        return Response.success();
    }

}

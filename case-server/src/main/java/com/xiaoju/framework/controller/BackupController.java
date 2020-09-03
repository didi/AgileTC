package com.xiaoju.framework.controller;

import com.xiaoju.framework.entity.CaseBackup;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.util.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(description = "case backup接口")
@RestController
@RequestMapping(value = "/api/backup")
public class BackupController {
    @Autowired
    CaseBackupService caseBackupService;

    @ApiOperation("获取用例历史记录")
    @ApiImplicitParam(name = "caseId", value = "caseId", required = true,paramType = "query",dataType = "long")
    @RequestMapping(value = "/getBackupByCaseId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<List<CaseBackup>> getBackupByCaseId(@RequestParam(value = "caseId") Long caseId,
                                                        @RequestParam(value = "beginTime",required = false) String beginTime,
                                                        @RequestParam(value = "endTime",required = false) String endTime) {
        List<CaseBackup> backupList =caseBackupService.getBackupByCaseId(caseId,beginTime,endTime);
        return Response.success(backupList);
    }

    @ApiOperation("删除用例历史记录")
    @ApiImplicitParam(name = "caseId", value = "caseId", required = true,paramType = "query",dataType = "long")
    @RequestMapping(value = "/deleteByCaseId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<Integer> deleteByCaseId(@RequestParam(value = "caseId") Long caseId) {
        Integer count =caseBackupService.deleteBackup(caseId);
        return Response.success(count);
    }

    @ApiOperation("添加用例历史记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseId", value = "用例id", required = true,paramType = "query",dataType = "long")})
    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<CaseBackup> addBackup(@RequestBody CaseBackup caseBackup) {
        CaseBackup caseBackup1 =caseBackupService.insertBackup(caseBackup);
        return Response.success(caseBackup1);
    }


}

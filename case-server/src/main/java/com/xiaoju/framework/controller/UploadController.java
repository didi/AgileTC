package com.xiaoju.framework.controller;

import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.service.WebSocketService;
import com.xiaoju.framework.util.ErrorCode;
import com.xiaoju.framework.util.FileUtil;
import com.xiaoju.framework.util.StatusCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;
import com.xiaoju.framework.util.Response;

/**
 *
 */
@Api(description = "文件导入导出接口")
@RestController
@CrossOrigin
@RequestMapping(value = "/api/file")
public class UploadController {
    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
    @Autowired
    WebSocketService webSocketService;

    @ApiOperation("根据xmind文件，导入用例")
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Response<Long> importXmindById(@RequestBody MultipartFile file,String creator,Long productLine,String requirementId,String caseTitle,String description,Integer channel,HttpServletRequest request) {

        if (request == null) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            request = requestAttributes.getRequest();
        }
        String username = request.getHeader("oe-username");
        if(username != null) {
            creator = username;
        }
        if (request.getContentType().toLowerCase().startsWith("multipart/")) {
            if (file.isEmpty()) {
                return Response.build(StatusCode.FILE_EMPTY_ERROR.getStatus(), StatusCode.FILE_EMPTY_ERROR.getMsg());
            }
            String fileName = file.getOriginalFilename();
            // 得到上传文件的扩展名
            String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            if (!fileExt.equals("xmind") && !fileExt.equals("zip")) {
                return Response.build(StatusCode.FILE_FORMATE_ERROR.getStatus(), StatusCode.FILE_FORMATE_ERROR.getMsg());
            }
            File file1 = new File("");
            String filePath = "";
            try {
                filePath = file1.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String desPath = filePath + "/temp/";

            File pathFile = new File(desPath);
            if(!pathFile.exists()){
                pathFile.mkdirs();
            }
            desPath = desPath + fileName;
            File dest = new File(desPath);
            String name[] = fileName.split("\\.");
            String title = name[0];
            Long time= new Date().getTime();

            String desc = filePath+"/temp/"+title+"_"+time.toString()+"/";

            try {
                file.transferTo(dest);
                boolean isSuccess = FileUtil.decompressZip(desPath, desc);
                if(isSuccess == false)
                {
                    return Response.build(StatusCode.FILE_IMPORT_ERROR.getStatus(), StatusCode.FILE_IMPORT_ERROR.getMsg());
                }
                LOG.info("文件上传成功，待解析");

                String jsonFileName = "content.json";
                String jsonFile = (desc+jsonFileName).replace("/", File.separator);
                File f = new File(jsonFile);
                TestCase testCaseResponse;
                if(f.exists()) {
                    testCaseResponse= webSocketService.getJsonByXmindJson(desc,creator,productLine,requirementId,caseTitle,description,channel);
                } else {
                    testCaseResponse = webSocketService.getJsonByXmind(desc, creator, productLine, requirementId, caseTitle, description,channel);
                }
                return Response.success(testCaseResponse.getId());

//                TestCase testCaseResponse= webSocketService.getJsonByXmind(desc,creator,productLine,requirementId,caseTitle,description);
//                if(testCaseResponse.getId()==1L)
//                {
//                   testCaseResponse= webSocketService.getJsonByXmindJson(desc,creator,productLine,requirementId,caseTitle,description);
//                }
//                return Response.success(testCaseResponse.getId());
            } catch (IOException e) {
                LOG.error(e.toString(), e);
                return Response.build(StatusCode.FILE_IMPORT_ERROR.getStatus(), StatusCode.FILE_IMPORT_ERROR.getMsg());

            }
        }
        return Response.build(StatusCode.FILE_IMPORT_ERROR.getStatus(),StatusCode.FILE_EMPTY_ERROR.getMsg());
    }

    @ApiOperation("根据id，导出用例")
    @RequestMapping(value = "/export", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void exportXmindById(@RequestParam(value = "id") Long id) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        HttpServletResponse response = requestAttributes.getResponse();
        response = webSocketService.getXmindById(id,response,request);
        response.setStatus(StatusCode.SERVICE_RUN_SUCCESS.getStatus());
    }
}
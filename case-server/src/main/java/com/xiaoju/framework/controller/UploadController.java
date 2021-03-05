package com.xiaoju.framework.controller;

import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.cases.FileImportReq;
import com.xiaoju.framework.entity.response.cases.ExportXmindResp;
import com.xiaoju.framework.entity.response.controller.Response;
import com.xiaoju.framework.service.FileService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.net.URLEncoder;

/**
 * 文件上传与导出
 *
 * @author didi
 * @date 2020/10/22
 */
@RestController
@CrossOrigin
@RequestMapping(value = "/api/file")
public class UploadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);

    @Resource
    FileService fileService;

    /**
     * 导入x-mind文件并且创建用例
     *
     * @param file 文件
     * @param creator 创建人
     * @param bizId 文件夹id
     * @param productLineId 业务线id
     * @param description 描述
     * @param channel 频道
     * @param requirementId 需求idStr
     * @return 响应体
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Long> importXmind(@RequestParam MultipartFile file, String creator, String bizId,
                                      Long productLineId, String title, String description, Integer channel, String requirementId) {
        FileImportReq req = new FileImportReq(file, creator, productLineId, title, description, channel, requirementId, bizId);
        req.validate();
        try {
            return Response.success(fileService.importXmindFile(req));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[导入x-mind出错] 传参req={},错误原因={}", req.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.FILE_IMPORT_ERROR.getStatus(), StatusCode.FILE_IMPORT_ERROR.getMsg());
        }
    }

    /**
     * 根据caseId导出用例
     * response 文件在http响应中输出
     *
     * @param id 用例id
     */
    @GetMapping(value = "/export")
    public void exportXmind(@RequestParam @NotNull(message = "用例id为空") Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            ExportXmindResp resp = fileService.exportXmindFile(id, request.getHeader(SystemConstant.HTTP_USER_AGENT));
            populateHttpResponse(response, resp.getData(), resp.getFileName());
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[导出x-mind错误] caseId={}, 错误原因={}", id, e.getMessage());
            e.printStackTrace();
            response.setStatus(StatusCode.FILE_IMPORT_ERROR.getStatus());
        }
    }

    /**
     * DispatchServlet手动扔出响应
     */
    private void populateHttpResponse(HttpServletResponse response, byte[] data, String fileName) throws Exception {
        response.setContentType("application/octet-stream; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; fileName=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setStatus(StatusCode.SERVICE_RUN_SUCCESS.getStatus());
        IOUtils.write(data, response.getOutputStream());
    }
}
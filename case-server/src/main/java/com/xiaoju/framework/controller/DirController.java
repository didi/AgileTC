package com.xiaoju.framework.controller;

import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.dir.DirCreateReq;
import com.xiaoju.framework.entity.request.dir.DirDeleteReq;
import com.xiaoju.framework.entity.request.dir.DirRenameReq;
import com.xiaoju.framework.entity.response.controller.Response;
import com.xiaoju.framework.service.DirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 文件夹
 *
 * @author didi
 * @date 2020/11/23
 */
@RestController
@RequestMapping("/api/dir")
public class DirController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirController.class);

    @Resource
    DirService dirService;

    /**
     * 获取业务线下的目录树
     *
     * @param productLineId 业务线id
     * @param channel 渠道
     * @return 响应体
     */
    @GetMapping(value = "/list")
    public Response<?> getDirTree(@RequestParam @NotNull(message = "业务线id为空") Long productLineId,
                                  @RequestParam @NotNull(message = "渠道为空") Integer channel) {
        return Response.success(dirService.getAllCaseDir(dirService.getDirTree(productLineId, channel)));
    }

    /**
     * 选中父节点，增加其下的文件夹
     * 创建同级 - 选中DirNode中的parentId
     * 创建子级 - 选在DirNode中的id
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/add")
    public Response<?> addDir(@RequestBody DirCreateReq request) {
        request.validate();
        try {
            return Response.success(dirService.addDir(request));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[Dir add]Add dir failed. params={} e={} ", request.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 重命名节点名称
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/rename")
    public Response<?> renameDir(@RequestBody DirRenameReq request) {
        request.validate();
        try {
            return Response.success(dirService.renameDir(request));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[Dir rename]rename dir failed. params={} e={} ", request.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 删除节点
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/delete")
    public Response<?> deleteDir(@RequestBody DirDeleteReq request) {
        request.validate();
        try {
            return Response.success(dirService.getAllCaseDir(dirService.delDir(request)));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[Dir delete]delete dir failed. params={} e={} ", request.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 新增、更新卡片下的目录树
     * 这里是返回没有头节点(顶级文件夹)的树
     *
     * @param productLineId 业务线id
     * @param channel 渠道
     * @return 响应体
     */
    @GetMapping(value = "/cardTree")
    public Response<?> getDirCardTree(@RequestParam @NotNull(message = "业务线id为空") Long productLineId,
                                      @RequestParam @NotNull(message = "渠道为空") Integer channel) {
        return Response.success(dirService.getDirTree(productLineId, channel));
    }
}

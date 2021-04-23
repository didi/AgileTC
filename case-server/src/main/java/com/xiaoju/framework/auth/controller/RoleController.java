package com.xiaoju.framework.auth.controller;

import com.xiaoju.framework.auth.entity.MinSubject;
import com.xiaoju.framework.auth.entity.RoleDeleteReq;
import com.xiaoju.framework.auth.entity.RoleUpsertReq;
import com.xiaoju.framework.auth.service.RoleService;
import com.xiaoju.framework.entity.response.controller.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 角色信息相关
 *
 * @author didi
 * @date 2021/2/2
 */
@RestController
@RequestMapping("/api/settings")
public class RoleController {

    @Resource
    private RoleService roleService;

    /**
     * 查看所有角色对应的用户  列表
     *
     * @param lineId 业务线
     * @param username 用户
     * @param channel 渠道
     * @return 响应体
     */
    @GetMapping("/role/list")
    @RequiresPermissions("setting:list")
    public Response<?> getRoleList(@RequestAttribute Long lineId, @RequestAttribute String username, @RequestAttribute Integer channel) {
        MinSubject subject = new MinSubject(username, lineId, channel);
        return Response.success(roleService.getRoleList(subject));
    }

    /**
     * 查看单条角色信息，包含所关联的角色、权限
     *
     * @param roleId 角色id，如果是模板角色id，会被拒绝
     * @param lineId 业务线
     * @param channel 渠道
     * @return 响应体
     */
    @GetMapping("/role/detail")
    @RequiresPermissions("setting:detail")
    public Response<?> getRoleDetail(@RequestParam @NotNull Long roleId, @RequestAttribute Long lineId, @RequestAttribute Integer channel) {
        MinSubject subject = new MinSubject(null, lineId, channel);
        return Response.success(roleService.getRoleDetail(subject, roleId));
    }

    /**
     * 查看所有可选的权限
     *
     * @return 列表
     */
    @GetMapping("/role/permList")
    @RequiresPermissions("setting:detail")
    public Response<?> getPermList() {
        return Response.success(roleService.getPermList());
    }

    /**
     * 新建或者更新角色
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping("/role/upsert")
    @RequiresPermissions("setting:upsert")
    public Response<?> createOrUpdateRole(@RequestBody RoleUpsertReq request) {
        request.validate();
        return Response.success(roleService.upsert(request));
    }

    /**
     * 删除某个角色实体，及其关联的用户和权限关系
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping("/role/delete")
    @RequiresPermissions("settings:delete")
    public Response<?> deleteRole(@RequestBody RoleDeleteReq request) {
        request.validate();
        return Response.success(roleService.delete(request));
    }

}

package com.xiaoju.framework.auth.service;

import com.xiaoju.framework.auth.entity.MinSubject;
import com.xiaoju.framework.auth.entity.PermResp;
import com.xiaoju.framework.auth.entity.RoleDeleteReq;
import com.xiaoju.framework.auth.entity.RoleDetailResp;
import com.xiaoju.framework.auth.entity.RoleListResp;
import com.xiaoju.framework.auth.entity.RoleUpsertReq;

import java.util.List;

/**
 * 角色相关校验
 *
 * @author didi
 * @date 2021/2/2
 */
public interface RoleService {

    /**
     * 获取角色列表
     *
     * @param subject 最小场景的主体
     * @return 角色列表，带上已关联用户
     */
    List<RoleListResp> getRoleList(MinSubject subject);

    /**
     * 获取角色列表
     *
     * @param roleId 角色id
     * @param subject 最小场景的主体
     * @return 角色详情，带上已关联用户和已关联权限
     */
    RoleDetailResp getRoleDetail(MinSubject subject, Long roleId);

    /**
     * 新增或者修改角色
     *
     * @param request 请求
     * @return id
     */
    int upsert(RoleUpsertReq request);

    /**
     * 删除角色
     *
     * @param request 请求体
     * @return id
     */
    int delete(RoleDeleteReq request);

    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    List<PermResp> getPermList();
}

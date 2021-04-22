package com.xiaoju.framework.auth.service.impl;

import com.xiaoju.framework.auth.constants.AuthConstant;
import com.xiaoju.framework.auth.entity.AgileToken;
import com.xiaoju.framework.auth.entity.MinSubject;
import com.xiaoju.framework.auth.entity.PermResp;
import com.xiaoju.framework.auth.entity.RoleDeleteReq;
import com.xiaoju.framework.auth.entity.RoleDetailResp;
import com.xiaoju.framework.auth.entity.RoleListResp;
import com.xiaoju.framework.auth.entity.RoleUpsertReq;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.auth.service.RoleService;
import com.xiaoju.framework.auth.service.helper.ShiroRedisHelper;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色实现类
 *
 * @author didi
 * @date 2021/2/2
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<RoleListResp> getRoleList(MinSubject subject) {
        return RoleListResp.buildMultiResp(userMapper.getRoleList(subject.getChannel(), subject.getLineId()));
    }

    @Override
    public RoleDetailResp getRoleDetail(MinSubject subject, Long roleId) {
        Role roleWithUsers = userMapper.getUsersByRoleId(subject.getChannel(), subject.getLineId(), roleId);
        Role roleWithPerms = userMapper.getPermsByRoleId(roleId);
        // 只需要检验一个就行了
        if (roleWithUsers == null || roleWithUsers.getId() == null) {
            throw new CaseServerException("角色实体不存在", StatusCode.INTERNAL_ERROR);
        }
        // 非模板用户禁止跨查询
        if (roleWithUsers.getType() == 0) {
            if (!roleWithUsers.getChannel().equals(subject.getChannel()) || !roleWithUsers.getLineId().equals(subject.getLineId())) {
                throw new CaseServerException("禁止跨渠道或者业务线查询", StatusCode.INTERNAL_ERROR);
            }
        }

        return RoleDetailResp.buildDetailResp(pack(roleWithUsers, roleWithPerms));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int upsert(RoleUpsertReq request) {
        AbstractRoleProcessor processor;
        if (request.getRoleId() == null) {
            //如果没有传入角色id，则是增加角色操作
            processor = new RoleCreateProcessor(request, userMapper);
        } else {
            //如果传入了角色id，则是更新原有角色操作
            processor = new RoleUpdateProcessor(request, userMapper);
        }
        processor.execute();
        return 1;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(RoleDeleteReq request) {
        Role role = userMapper.getUsersByRoleId(request.getChannel(), request.getLineId(), request.getRoleId());

        checkRoleExists(role);
        checkRoleNormal(role);
        checkRoleBlock(role);

        if (!role.getChannel().equals(request.getChannel()) || !role.getLineId().equals(request.getLineId())) {
            throw new CaseServerException("禁止跨渠道或者业务线删除角色", StatusCode.INTERNAL_ERROR);
        }

        //删除数据库中的角色信息
        deleteRoleAndRelation(role, request.getUsername());

        //删除缓存中的认证数据和授权数据
        List<User> users = role.getUsers();
        for (User user : users) {
            ShiroRedisHelper.delete(new AgileToken(user.getUsername(),request.getChannel().toString(),request.getLineId().toString()));
        }

        return role.getId().intValue();
    }

    @Override
    public List<PermResp> getPermList() {
        return PermResp.buildMultiResp(userMapper.getPermList());
    }

    public static void checkRoleExists(Role role) {
        if (role == null || SystemConstant.IS_DELETE.equals(role.getIsDelete())) {
            throw new CaseServerException("当前角色不存在", StatusCode.INTERNAL_ERROR);
        }
    }

    public static void checkRoleNormal(Role role) {

        //role.getType() > 0 表示该角色为系统设置的五种默认角色，不可以被删除。
        if (role == null || role.getType() > 0) {
            throw new CaseServerException("当前角色不支持删除", StatusCode.INTERNAL_ERROR);
        }
    }

    public static void checkRoleBlock(Role role) {
        if (role != null && AuthConstant.BLOCKED.equals(role.getIsBlock())) {
            throw new CaseServerException("当前角色被锁定,无法修改", StatusCode.INTERNAL_ERROR);
        }
    }

    public void deleteRoleAndRelation(Role role, String username) {
        userMapper.deleteRole(role.getId(), username);
        userMapper.deleteUserRoleRel(role.getId(), username);
        userMapper.deleteRolePermRel(role.getId(), username);

        // 同时这步操作要删除数据库中该角色关联的所有用户
        if (!CollectionUtils.isEmpty(role.getUsers())) {
            userMapper.batchDeleteUsers(role.getUsers());
        }
    }

    private Role pack(Role withUsers, Role withPerms) {
        withUsers.setPermissions(withPerms.getPermissions());
        return withUsers;
    }

}

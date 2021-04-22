package com.xiaoju.framework.auth.entity;

import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.entity.response.PersonResp;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色详情
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
public class RoleDetailResp {

    private Long roleId;

    private String roleName;

    private List<PersonResp> users;

    private List<PermResp> perms;

    public static RoleDetailResp buildDetailResp(Role role) {
        RoleDetailResp resp = new RoleDetailResp();
        resp.setRoleId(role.getId());
        resp.setRoleName(role.getRoleName());
        resp.setUsers(role.getUsers().stream().map(PersonResp::buildPersonResp).collect(Collectors.toList()));
        resp.setPerms(role.getPermissions().stream().map(PermResp::buildPermResp).collect(Collectors.toList()));
        return resp;
    }
}


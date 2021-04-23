package com.xiaoju.framework.auth.service.impl;

import com.xiaoju.framework.auth.entity.RoleUpsertReq;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.mapper.UserMapper;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO
 *
 * @author didi
 * @date 2021/2/3
 */
public abstract class AbstractRoleProcessor {

    protected RoleUpsertReq request;

    protected UserMapper userMapper;

    protected Role role;

    protected abstract void saveRole();

    protected abstract void saveUsers();

    protected abstract void saveUserRoleRelations();

    protected abstract void saveRolePermsRelations();



    public void execute() {
        saveRole();
        saveUsers();
        saveUserRoleRelations();
        saveRolePermsRelations();
    }

    public AbstractRoleProcessor(RoleUpsertReq request, UserMapper userMapper) {
        this.request = request;
        this.userMapper = userMapper;
    }

    public RoleUpsertReq getRequest() {
        return request;
    }

    public void setRequest(RoleUpsertReq request) {
        this.request = request;
    }

    public UserMapper getUserMapper() {
        return userMapper;
    }

    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<Long> getPermIds() {
        if (StringUtils.isEmpty(request.getPermIds())) {
            return new HashSet<>();
        }

        return Stream.of(request.getPermIds().split(SystemConstant.COMMA)).map(Long::valueOf).collect(Collectors.toSet());
    }

    public Set<String> getUsernames() {
        if (StringUtils.isEmpty(request.getUsers())) {
            return new HashSet<>();
        }

        return Stream.of(request.getUsers().split(SystemConstant.COMMA)).collect(Collectors.toSet());
    }
}

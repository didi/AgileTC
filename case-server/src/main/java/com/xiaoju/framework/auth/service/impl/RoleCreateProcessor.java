package com.xiaoju.framework.auth.service.impl;

import com.xiaoju.framework.auth.entity.RoleUpsertReq;
import com.xiaoju.framework.auth.entity.pojo.Permission;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.auth.entity.pojo.RolePermRel;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.auth.entity.pojo.UserRoleRel;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.mapper.UserMapper;
import org.apache.commons.compress.utils.Lists;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新增处理器
 *
 * @author didi
 * @date 2021/2/3
 */
public class RoleCreateProcessor extends AbstractRoleProcessor {

    public RoleCreateProcessor(RoleUpsertReq request, UserMapper userMapper) {
        super(request, userMapper);
    }

    private List<User> insertUsers = new ArrayList<>();

    @Override
    protected void saveRole() {
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setIsDelete(SystemConstant.NOT_DELETE);
        role.setIsBlock(0);
        // 自定义type=0
        role.setType(0);
        role.setLineId(request.getLineId());
        role.setChannel(request.getChannel());
        role.setCreator(request.getUsername());
        role.setModifier(request.getUsername());
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        userMapper.insertRole(role);
        this.role = role;
    }

    @Override
    protected void saveUsers() {
        Set<String> inputNames = getUsernames();
        if (StringUtils.isEmpty(request.getUsername())) {
            // 可以关联空用户
            return ;
        }

        // 防止最小场景下，用户有多个角色，这里进行拦截
        //贾：一个用户在一个最小场景下只能有一个角色，也就是说一个用户在channel和lineid相同的情况下，只能关联一个角色。
        List<User> dbUsers = userMapper.getMinorUserCollection(inputNames, request.getChannel(), request.getLineId());
        Set<String> dbUsernames = dbUsers.stream().map(User::getUsername).collect(Collectors.toSet());

        for (String inputName : inputNames) {

            //如果数据库用户表中已经存在该用户则报错
            if (dbUsernames.contains(inputName)) {
                throw new CaseServerException("用户:" + inputName + "有重复角色存在", StatusCode.INTERNAL_ERROR);
            }

            //创建新用户，放入数据库用户表中
            User user = new User();
            user.setUsername(inputName);
            user.setIsBlock(0);
            user.setIsDelete(SystemConstant.NOT_DELETE);
            user.setLineId(request.getLineId());
            user.setChannel(request.getChannel());
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            this.insertUsers.add(user);
        }

        if (insertUsers.size() > 0) {
            userMapper.batchInsertUsers(this.insertUsers);
        }
    }

    @Override
    protected void saveUserRoleRelations() {
        List<UserRoleRel> relations = new ArrayList<>();
        for (User user : this.insertUsers) {
            UserRoleRel rel = new UserRoleRel();
            rel.setUserId(user.getId());
            rel.setRoleId(role.getId());
            rel.setIsDelete(SystemConstant.NOT_DELETE);
            rel.setCreator(request.getUsername());
            rel.setModifier(request.getUsername());
            rel.setChannel(request.getChannel());
            rel.setLineId(request.getLineId());
            rel.setCreateTime(new Date());
            rel.setUpdateTime(new Date());
            relations.add(rel);
        }
        if (relations.size() > 0) {
            userMapper.batchInsertUserRoleRel(relations);
        }
    }

    @Override
    protected void saveRolePermsRelations() {
        Set<Long> permsIds = getPermIds();

        if (CollectionUtils.isEmpty(permsIds)) {
            throw new CaseServerException("角色必须关联至少一个权限", StatusCode.INTERNAL_ERROR);
        }

        // 做一层校验，防止塞入不可信的permId
        List<Permission> perms = userMapper.getPermList();
        Set<Long> dbPermIds = perms.stream().filter(p -> SystemConstant.NOT_DELETE.equals(p.getIsDelete())).map(Permission::getId).collect(Collectors.toSet());

        List<RolePermRel> list = Lists.newArrayList();
        for (Long permId : permsIds) {
            if (dbPermIds.contains(permId)) {
                RolePermRel rel = new RolePermRel();
                rel.setPermId(permId);
                rel.setRoleId(role.getId());
                rel.setIsDelete(SystemConstant.NOT_DELETE);
                rel.setCreator(request.getUsername());
                rel.setModifier(request.getUsername());
                rel.setCreateTime(new Date());
                rel.setUpdateTime(new Date());
                list.add(rel);
            }
        }
        if (list.size() > 0) {
            userMapper.batchInsertRolePermRel(list);
        }
    }
}

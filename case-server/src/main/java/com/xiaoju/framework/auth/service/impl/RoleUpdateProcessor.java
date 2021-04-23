package com.xiaoju.framework.auth.service.impl;

import com.xiaoju.framework.auth.entity.AgileToken;
import com.xiaoju.framework.auth.entity.RoleUpsertReq;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.auth.entity.pojo.RolePermRel;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.auth.entity.pojo.UserRoleRel;
import com.xiaoju.framework.auth.service.helper.ShiroRedisHelper;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.mapper.UserMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author didi
 * @date 2021/2/3
 */
public class RoleUpdateProcessor extends AbstractRoleProcessor {

    public RoleUpdateProcessor(RoleUpsertReq request, UserMapper userMapper) {
        super(request, userMapper);
    }

    private List<User> insertUsers = new ArrayList<>();

    /**
     * 这里不会删除实际的用户，而是去删除用户与角色之间的关联
     */
    private List<UserRoleRel> abandomRoleRel = new ArrayList<>();

    @Override
    protected void saveRole() {
        Role role = userMapper.selectRole(request.getRoleId());
        checkRoleExists(role);

        //role.setRoleName(role.getType() == 0 ? request.getRoleName() : role.getRoleName());

        //如果角色是基本角色且角色名称被修改了，则报错提示基础角色不能被修改
        if (role.getType() > 0 && !request.getRoleName().equals(role.getRoleName())) {
            throw new CaseServerException("系统角色名称不得被修改",StatusCode.INTERNAL_ERROR);
        }else {
            role.setRoleName(request.getRoleName());
        }


        role.setModifier(request.getUsername());
        role.setUpdateTime(new Date());
        userMapper.updateRole(role);
        this.role = role;
    }

    @Override
    protected void saveUsers() {
        Set<String> inputNames = getUsernames();

        // 首先，找到db中所有当前场景下的人
        List<User> dbUsers = userMapper.getMinorAllUserCollection(request.getChannel(), request.getLineId());
        List<UserRoleRel> relations = dbUsers.size() > 0 ? userMapper.getUserRoleRelByUserIdIn(dbUsers.stream().map(User::getId).collect(Collectors.toList())) : new ArrayList<>();

        // 然后，封装成<userId, entity>，如果发现roleId不一致的，则视为重复用户
        Map<Long, UserRoleRel> dbRelSet = relations.stream().collect(Collectors.toMap(UserRoleRel::getUserId, Function.identity()));

        Map<String, User> dbUserMap = dbUsers.stream().collect(Collectors.toMap(User::getUsername, Function.identity()));
        Set<String> dbUserSet = dbUserMap.keySet();

        // 先找到新用户
        for (String inputName : inputNames) {

            // 传入的内容中，如果在dbUser中找不到，那就是新的用户
            // 如果有，就是老用户，执行更新
            if (dbUserSet.contains(inputName)) {
                User dbUser = dbUserMap.get(inputName);
                UserRoleRel roleRel = dbRelSet.get(dbUser.getId());

                // 即使是老用户，也要看是不是赋予了其第二个权限，如果重复了就要报错
                if (roleRel == null) {
                    throw new CaseServerException("用户角色存在异常，请联系后台排查", StatusCode.INTERNAL_ERROR);
                }
                if (!roleRel.getRoleId().equals(request.getRoleId())) {
                    throw new CaseServerException("用户:" + inputName + "有重复角色存在", StatusCode.INTERNAL_ERROR);
                }

                //删除缓存中的认证数据和授权数据
                ShiroRedisHelper.delete(new AgileToken(dbUser.getUsername(),request.getChannel().toString(),request.getLineId().toString()));

                continue;
            }

            //创建新用户
            User user = new User();
            user.setUsername(inputName);
            user.setIsBlock(0);
            user.setIsDelete(SystemConstant.NOT_DELETE);
            user.setLineId(request.getLineId());
            user.setChannel(request.getChannel());
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            userMapper.insertUser(user);
            this.insertUsers.add(user);

        }

        // 再看有哪些人是被删除的
        List<User> deleteUsers = new ArrayList<>();
        for (String dbUsername : dbUserSet) {
            if (!inputNames.contains(dbUsername)) {
                User delUser = dbUserMap.get(dbUsername);
                UserRoleRel rel = dbRelSet.get(delUser.getId());
                if (!rel.getRoleId().equals(request.getRoleId())) {
                    continue;
                }
                deleteUsers.add(delUser);
                rel.setModifier(request.getUsername());
                rel.setUpdateTime(new Date());
                this.abandomRoleRel.add(rel);
            }
        }

        // 删除被删除的用户
        if (deleteUsers.size() > 0) {

            //删除缓存中的认证数据和授权数据
            for (User user : deleteUsers) {
                ShiroRedisHelper.delete(new AgileToken(user.getUsername(),request.getChannel().toString(),request.getLineId().toString()));
            }

            //将数据库中这些用户删除
            userMapper.batchDeleteUsers(deleteUsers);
        }
    }

    @Override
    protected void saveUserRoleRelations() {
        List<UserRoleRel> addRelations = new ArrayList<>();
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
            addRelations.add(rel);
        }
        if (addRelations.size() > 0) {
            userMapper.batchInsertUserRoleRel(addRelations);
        }
        if (this.abandomRoleRel.size() > 0) {
            userMapper.batchDeleteUsersRoleRel(this.abandomRoleRel);
        }
    }

    @Override
    protected void saveRolePermsRelations() {

//        if (role.getType() != 0) {
//            return;
//        }

        List<RolePermRel> dbrels = userMapper.getRolePermRelByRoleId(role.getId());
        Set<Long> inputPermIds = getPermIds();
        // <dbPermId, entity>
        Map<Long, RolePermRel> dbRelMap = dbrels.stream().collect(Collectors.toMap(RolePermRel::getPermId, Function.identity()));
        Set<Long> dbPermIds = dbRelMap.keySet();

        //如果修改了基础角色的权限则报错
        if (role.getType() != 0) {
            for (Long inputPermId : inputPermIds) {
                if (!dbPermIds.contains(inputPermId)) {
                    throw new CaseServerException("系统角色权限不得被修改",StatusCode.INTERNAL_ERROR);
                }
            }
            for (Long dbPermId : dbPermIds) {
                if (!inputPermIds.contains(dbPermId)) {
                    throw new CaseServerException("系统角色权限不得被修改",StatusCode.INTERNAL_ERROR);
                }
            }
        }

        List<RolePermRel> insertRelList = new ArrayList<>();
        for (Long inputPermId : inputPermIds) {
            RolePermRel rolePermRel = dbRelMap.get(inputPermId);
            if (rolePermRel == null) {
                RolePermRel newRel = new RolePermRel();
                newRel.setPermId(inputPermId);
                newRel.setRoleId(role.getId());
                newRel.setIsDelete(SystemConstant.NOT_DELETE);
                newRel.setCreator(request.getUsername());
                newRel.setModifier(request.getUsername());
                newRel.setCreateTime(new Date());
                newRel.setUpdateTime(new Date());
                insertRelList.add(newRel);
            }
        }

        List<RolePermRel> deleteRelList = new ArrayList<>();
        for (Long dbPermId : dbPermIds) {
            if (!inputPermIds.contains(dbPermId)) {
                RolePermRel rel = dbRelMap.get(dbPermId);
                rel.setModifier(request.getUsername());
                rel.setUpdateTime(new Date());
                deleteRelList.add(rel);
            }
        }

        if (insertRelList.size() > 0) {
            userMapper.batchInsertRolePermRel(insertRelList);
        }
        if (deleteRelList.size() > 0) {
            userMapper.batchDeleteRolePermRel(deleteRelList);
        }
    }

    private void checkRoleExists(Role role) {
        if (role == null || SystemConstant.IS_DELETE.equals(role.getIsDelete())) {
            throw new CaseServerException("当前角色不存在", StatusCode.INTERNAL_ERROR);
        }
    }
}

package com.xiaoju.framework.mapper;

import com.xiaoju.framework.auth.entity.pojo.Permission;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.auth.entity.pojo.RolePermRel;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.auth.entity.pojo.UserRoleRel;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * 用户、角色、权限相关的数据库查询
 *
 * @author didi
 * @date 2021/1/21p
 */
@Repository
public interface UserMapper {

    /**
     * 获取用户信息,顺便带上权限
     *
     * @param username 用户名
     * @param channel 渠道
     * @param lineId 业务线id
     * @return user-[perms]
     */
    User getUserWithPerms(String username, Integer channel, Long lineId);

    /**
     * 获取用户信息,顺便带上角色
     *
     * @param username 用户名
     * @param channel 渠道
     * @param lineId 业务线id
     * @return user-角色
     */
    User getUserWithRoleAndPerms(String username, Integer channel, Long lineId);

    /**
     * 单纯获取用户信息
     *
     * @param username 用户名
     * @param channel 渠道
     * @param lineId 业务线id
     * @return user
     */
    User getUser(String username, Integer channel, Long lineId);

    /**
     * 获取当前场景下，根据输入的用户名，去获取所有的用户
     *
     * @param usernames 用户名即可
     * @param channel 渠道
     * @param lineId 业务线id
     * @return user
     */
    List<User> getMinorUserCollection(Set<String> usernames, Integer channel, Long lineId);

    /**
     * 获取当前场景下，所有的用户
     *
     * @param channel 渠道
     * @param lineId 业务线id
     * @return user
     */
    List<User> getMinorAllUserCollection(Integer channel, Long lineId);

    /**
     * 获取默认角色-普通用户
     * @return 默认角色
     */
    Role getDefaultRole();

    /**
     * 单条查询
     * @param roleId 角色id
     * @return 默认角色
     */
    Role selectRole(Long roleId);

    /**
     * 插入新用户
     *
     * @param newUser 用户实体
     */
    void insertUser(User newUser);

    /**
     * 插入新用户
     *
     * @param list 新用户
     */
    void batchInsertUsers(List<User> list);

    /**
     * 插入用户-角色关联条目
     *
     * @param rel 关系实体
     */
    void insertUserRoleRel(UserRoleRel rel);

    /**
     * 批量插入用户-角色关联条目
     *
     * @param list 关联列表
     */
    void batchInsertUserRoleRel(List<UserRoleRel> list);

    /**
     * 批量插入角色-权限关联条目
     *
     * @param list 关联列表
     */
    void batchInsertRolePermRel(List<RolePermRel> list);

    /**
     * 插入新角色
     *
     * @param role 角色实体
     */
    void insertRole(Role role);

    /**
     * 更新角色
     *
     * @param role 角色实体
     */
    void updateRole(Role role);

    /**
     * 获取当前场景下所有的用户
     *
     * @param channel 渠道
     * @param lineId 业务线
     * @return 列表
     */
    List<Role> getRoleList(Integer channel, Long lineId);

    /**
     * 获取某个角色在当前场景下的所有用户
     *
     * @param channel 渠道
     * @param lineId 业务线
     * @param roleId 角色id
     * @return 列表
     */
    Role getUsersByRoleId(Integer channel, Long lineId, Long roleId);

    /**
     * 获取某个角色在当前场景下的所有资源
     *
     * @param roleId 角色id
     * @return 列表
     */
    Role getPermsByRoleId(Long roleId);

    /**
     * 删除自定义角色
     *
     * @param roleId 自定义的角色  的 角色id
     * @param name 用户名
     */
    void deleteRole(Long roleId, String name);

    /**
     * 删除自定义角色关联的用户关系
     *
     * @param roleId 自定义的角色  的 角色id
     * @param name 用户名
     */
    void deleteUserRoleRel(Long roleId, String name);

    /**
     * 删除自定义角色关联的权限关系
     *
     * @param roleId 自定义的角色  的 角色id
     * @param name 用户名
     */
    void deleteRolePermRel(Long roleId, String name);

    void batchDeleteUsers(List<User> users);

    void batchDeleteUsersRoleRel(List<UserRoleRel> rels);

    void batchDeleteRolePermRel(List<RolePermRel> rels);

    /**
     * 获取所有权限内容
     *
     * @return 列表
     */
    List<Permission> getPermList();

    List<UserRoleRel> getUserRoleRelByUserIdIn(List<Long> userIds);

    List<RolePermRel> getRolePermRelByRoleId(Long role);

    /**
     * 获取包括密码和盐值的User对象
     * @param username
     * @return
     */
    User getUserByName(String username);

    /**
     * 插入包括密码和盐值得User对象
     * @param user
     */
    void insertUserWithPassword(User user);

    /**
     * 获取用户信息
     * @param username
     * @param roleId
     * @param beginTime
     * @param endTime
     * @return
     */
    List<User> getUserListWithRole(@Param("username") String username,
                                   @Param("roleId") Long roleId,
                                   @Param("beginTime") Date beginTime,
                                   @Param("endTime") Date endTime);

    /**
     * 通过用户id获取对应角色
     * @param userId
     * @return
     */
    Role getRoleByUserId(Long userId);

    /**
     * 根据用户id删除用户
     * @param userId
     * @return
     */
    int deleteUser(Long userId);

    /**
     * 根据用户id删除用户角色关联
     * @param userId
     */
    void deleteUserRoleRelByUserId(Long userId);

    /**
     * 根据用户id修改用户关联角色
     * @param userId
     * @param newRoleId
     * @param modifier
     * @return
     */
    int updateUserRole(@Param("userId") Long userId,
                       @Param("newRoleId") Long newRoleId,
                       @Param("modifier") String modifier);
}

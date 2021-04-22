package com.xiaoju.framework.auth.service;

import com.xiaoju.framework.auth.entity.UserDeleteReq;
import com.xiaoju.framework.auth.entity.UserListReq;
import com.xiaoju.framework.auth.entity.UserListResp;
import com.xiaoju.framework.auth.entity.UserLoginReq;
import com.xiaoju.framework.auth.entity.UserRegisterReq;
import com.xiaoju.framework.auth.entity.UserRoleUpdateReq;
import com.xiaoju.framework.entity.response.controller.PageModule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/13 14:22:04
 */
public interface UserService {

    /**
     * 新用户注册
     * @param req
     * @return
     */
    Integer register(UserRegisterReq req, HttpServletRequest request, HttpServletResponse response);

    /**
     * 用户登录
     * @param req
     * @return
     */
    Integer login(UserLoginReq req, HttpServletRequest request, HttpServletResponse response);

    /**
     * 用户查询
     * @param req
     * @return
     */
    PageModule<UserListResp> getUserList(UserListReq req);

    /**
     * 根据用户id删除用户
     * @param req
     * @return
     */
    int userDelete(UserDeleteReq req);

    /**
     * 根据用户id修改用户角色
     * @param request
     * @return
     */
    int userRoleUpdate(UserRoleUpdateReq req,String username);
}

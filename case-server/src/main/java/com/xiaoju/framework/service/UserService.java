package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.request.auth.UserLoginReq;
import com.xiaoju.framework.entity.request.auth.UserRegisterReq;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created by didi on 2021/4/22.
 */
public interface UserService {
    Integer register(UserRegisterReq req, HttpServletRequest request, HttpServletResponse response);
    Integer login(UserLoginReq req, HttpServletRequest request, HttpServletResponse response);
    Integer logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取用户对应角色权限集
     *
     * @param username 用户名称
     * @return 角色权限集
     */
    List<String> getUserRoleAuthority(String username);
}

package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.request.auth.UserLoginReq;
import com.xiaoju.framework.entity.request.auth.UserRegisterReq;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by didi on 2021/4/22.
 */
public interface UserService {
    Integer register(UserRegisterReq req, HttpServletRequest request, HttpServletResponse response);
    Integer login(UserLoginReq req, HttpServletRequest request, HttpServletResponse response);
    Integer logout(HttpServletRequest request, HttpServletResponse response);
}

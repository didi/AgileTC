package com.xiaoju.framework.auth.controller;

import com.xiaoju.framework.auth.entity.UserDeleteReq;
import com.xiaoju.framework.auth.entity.UserListReq;
import com.xiaoju.framework.auth.entity.UserLoginReq;
import com.xiaoju.framework.auth.entity.UserRegisterReq;
import com.xiaoju.framework.auth.entity.UserRoleUpdateReq;
import com.xiaoju.framework.auth.service.UserService;
import com.xiaoju.framework.entity.response.controller.Response;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/13 13:47:05
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final String USERNAME = "username";

    private static final String JSESSIONID = "JSESSIONID";

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param req
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/register")
    public Response<?> register(@RequestBody UserRegisterReq req, HttpServletRequest request, HttpServletResponse response) {
        return Response.success(userService.register(req,request,response));
    }

    /**
     * 用户登录
     * @param req
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/login")
    public Response<?> login(@RequestBody UserLoginReq req, HttpServletRequest request,HttpServletResponse response) {
        return Response.success(userService.login(req,request,response));
    }

    /**
     * 查询用户列表
     * @param username
     * @param roleId
     * @param beginTime
     * @param endTime
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    public Response<?> getUserList(@RequestParam(value = "username",required = false) String username,
                                   @RequestParam(value = "roleId",required = false) Long roleId,
                                   @RequestParam(value = "beginTime",required = false) String beginTime,
                                   @RequestParam(value = "endTime",required = false) String endTime,
                                   @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                   @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize) {
        return Response.success(userService.getUserList(
            new UserListReq(username,roleId,beginTime,endTime,pageNum,pageSize)));
    }

    /**
     * 根据用户id删除用户
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public Response<?> userDelete(@RequestBody UserDeleteReq request) {
        return Response.success(userService.userDelete(request));
    }

    /**
     * 根据用户id和角色id修改用户的角色
     * @param request
     * @return
     */
    @PostMapping("/update")
    public Response<?> userRoleUpdate(@RequestBody UserRoleUpdateReq req,HttpServletRequest request) {

        //从header中获取操作用户名
        String username = request.getHeader(USERNAME);

        return Response.success(userService.userRoleUpdate(req,username));
    }


    @PostMapping("/quit")
    public Response<?> userQuit(HttpServletRequest request,HttpServletResponse response) {
        //获取所有cookie并将其value设置为null
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            //删除cookie中的username
            if (cookie.getName().equals(USERNAME)) {
                Cookie newcookie = new Cookie(cookie.getName(),null);

                //路径path相同才会被判定为同名cookie，才能达到覆盖效果
                newcookie.setPath("/");
                newcookie.setMaxAge(0);

                response.addCookie(newcookie);
            }

            //删除cookie中的jsessionid
            if (cookie.getName().equals(JSESSIONID)) {
                Cookie newcookie = new Cookie(cookie.getName(),null);

                //路径path相同才会被判定为同名cookie，才能达到覆盖效果
                newcookie.setPath("/");
                newcookie.setMaxAge(0);

                response.addCookie(newcookie);
            }
        }

        return Response.success(null);
    }
}

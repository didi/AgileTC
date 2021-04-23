package com.xiaoju.framework.auth.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaoju.framework.auth.entity.UserDeleteReq;
import com.xiaoju.framework.auth.entity.UserListReq;
import com.xiaoju.framework.auth.entity.UserListResp;
import com.xiaoju.framework.auth.entity.UserLoginReq;
import com.xiaoju.framework.auth.entity.UserRegisterReq;
import com.xiaoju.framework.auth.entity.UserRoleUpdateReq;
import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.auth.entity.pojo.UserRoleRel;
import com.xiaoju.framework.auth.service.UserService;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.response.controller.PageModule;
import com.xiaoju.framework.mapper.UserMapper;
import com.xiaoju.framework.util.CodecUtils;
import com.xiaoju.framework.util.CookieUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/13 14:39:56
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Long VISITORID = 13L;

    private static final Long ADMINISTRATOR = 10L;

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer register(UserRegisterReq req, HttpServletRequest request, HttpServletResponse response) {

        //1.检查数据库中是否已经存在该用户
        User dbuser = userMapper.getUserByName(req.getUsername());
        if (dbuser != null) {
            throw new CaseServerException("用户名已存在", StatusCode.INTERNAL_ERROR);
        }

        User user = new User();

        //2.生成盐，对密码进行加密再保存到数据库中
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        user.setPassword(CodecUtils.md5Hex(req.getPassword(),salt));

        user.setUsername(req.getUsername());
        user.setChannel(1);
        user.setLineId(1L);
        user.setIsBlock(0);
        user.setIsDelete(SystemConstant.NOT_DELETE);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        //3.将新用户数据保存到数据库中
        userMapper.insertUserWithPassword(user);

        //4.将新用户关联到对应角色
        User dbNewUser = userMapper.getUserByName(req.getUsername());
        UserRoleRel userRoleRel = new UserRoleRel();
        userRoleRel.setUserId(dbNewUser.getId());

        //关联到访客
        //userRoleRel.setRoleId(VISITORID);

        //关联到超级管理员
        userRoleRel.setRoleId(ADMINISTRATOR);

        userRoleRel.setIsDelete(SystemConstant.NOT_DELETE);
        userRoleRel.setCreateTime(new Date());
        userRoleRel.setUpdateTime(new Date());
        userRoleRel.setCreator(req.getUsername());
        userRoleRel.setModifier(req.getUsername());
        userRoleRel.setChannel(dbNewUser.getChannel());
        userRoleRel.setLineId(dbNewUser.getLineId());
        userMapper.insertUserRoleRel(userRoleRel);

        //5.将新用户设置到cookie中去
        CookieUtils.setCookie(request,response,"username",req.getUsername(),60 * 60 * 24,null,false);

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer login(UserLoginReq req, HttpServletRequest request, HttpServletResponse response) {

        //1.检查数据库中是否存在该用户
        User dbuser = userMapper.getUserByName(req.getUsername());
        if (dbuser == null) {
            throw new CaseServerException("用户名不存在", StatusCode.INTERNAL_ERROR);
        }

        //2.校验密码是否正确
        if (!dbuser.getPassword().equals(CodecUtils.md5Hex(req.getPassword(),dbuser.getSalt()))) {
            throw new CaseServerException("密码错误",StatusCode.INTERNAL_ERROR);
        }

        //3.将新用户设置到cookie中去
        CookieUtils.setCookie(request,response,"username",req.getUsername(),60 * 60 * 24,null,false);

        return null;
    }

    /**
     * 查询用户列表
     * @param req
     * @return
     */
    @Override
    public PageModule<UserListResp> getUserList(UserListReq req) {

        PageModule<UserListResp> res = new PageModule<>();

        Date beginTime = transforTime(req.getBeginTime());
        Date endTime = transforTime(req.getEndTime());

        //分页
        PageHelper.startPage(req.getPageNum(),req.getPageSize());

        List<User> users = userMapper.getUserListWithRole(req.getUsername(),req.getRoleId(),beginTime,endTime);

        if (!CollectionUtils.isEmpty(users)) {
            List<UserListResp> list = new ArrayList<>();
            for (User user : users) {
                UserListResp resp = new UserListResp();
                resp.setUserId(user.getId());
                resp.setUsername(user.getUsername());

                Role role = userMapper.getRoleByUserId(user.getId());
                resp.setRoleId(role.getId());
                resp.setRoleName(role.getRoleName());

                list.add(resp);
            }

            res = PageModule.buildPage(list, ((Page<User>) users).getTotal());
        }

        return res;
    }

    /**
     * 根据用户id删除用户
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int userDelete(UserDeleteReq req) {
        //删除用户表中用户
        int res = userMapper.deleteUser(req.getUserId());

        //删除用户角色关联表中用户与角色的关联
        userMapper.deleteUserRoleRelByUserId(req.getUserId());

        return res;
    }

    /**
     * 根据用户id修改用户角色
     * @param req
     * @param username
     * @return
     */
    @Override
    public int userRoleUpdate(UserRoleUpdateReq req,String username) {
        if (req.getNewRoleId().equals(req.getOldRoleId())) {
            throw new CaseServerException("角色未修改",StatusCode.INTERNAL_ERROR);
        }

        int res = userMapper.updateUserRole(req.getUserId(),req.getNewRoleId(),username);
        return res;
    }

    /**
     * 将字符串类型时间戳转换成"yyyy-MM-dd HH:mm:ss"格式Date类型时间戳
     * @param timeStr
     * @return
     */
    private Date transforTime(String timeStr) {
        if (!StringUtils.isEmpty(timeStr)) {
            try {
                return new SimpleDateFormat(DEFAULT_PATTERN).parse(timeStr);
            }catch (ParseException e) {
                throw new CaseServerException("日期类型转换错误",StatusCode.INTERNAL_ERROR);
            }
        }
        return null;
    }
}

package com.xiaoju.framework.auth.filter;

import com.xiaoju.framework.auth.constants.AuthConstant;
import com.xiaoju.framework.auth.entity.AgileToken;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基于header的拦截器
 *
 * @author jiazhengyang
 * @date 2021/4/12
 */
public class AuthFilter extends BasicHttpAuthenticationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private static final String LOGIN_SIGN = "username";

    /**
     * 判断请求头中有没有用户名请求头，有则说明该请求需要进行登录校验。
     * @param request
     * @param response
     * @return
     */
    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        return StringUtils.hasText(req.getHeader(LOGIN_SIGN));
    }

    /**
     * 登录校验
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;

        // filter不会去拦截ws请求
        String username = req.getHeader(LOGIN_SIGN);

//        // 当请求的header中没有上述参数，说明这是一个ws请求，直接抛出相关异常，不会执行后续拦截操作。
//        if (StringUtils.isEmpty(username)) {
//            throw new CaseServerException("User Unauthenticated", StatusCode.INTERNAL_ERROR);
//        }

        // 直接使用用户名来做token
        AgileToken token = new AgileToken(username, AuthConstant.DEFAULTCHANNEL,AuthConstant.DEFAULTLINEID);

        // 获取用户实例
        Subject subject = getSubject(request, response);
        // 登录
        subject.login(token);
        return true;
    }

    /**
     * 校验用户是否正确登录
     * @param request
     * @param response
     * @param mappedValue
     * @return
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        if (isLoginAttempt(request, response)) {
            try {
                executeLogin(request, response);
            } catch (Exception e) {
                LOGGER.error("登录出错,错误原因={}", e.getLocalizedMessage());
                e.printStackTrace();
                throw new CaseServerException(StatusCode.AUTH_UNKNOWN);
            }
        }
        return true;
    }

    /**
     * 先对请求进行数据封装，然后交给过滤器。
     * @param request
     * @param response
     * @param chain
     * @throws Exception
     */
    @Override
    protected void executeChain(ServletRequest request, ServletResponse response, FilterChain chain) throws Exception {
        ShiroHttpServletRequest req = (ShiroHttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse) response;

        chain.doFilter(req, resp);
    }
}

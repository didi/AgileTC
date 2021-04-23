package com.xiaoju.framework.filter;

import com.xiaoju.framework.util.CookieUtils;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未登录请求重定向到登录页面的过滤器
 *
 * @author littleforestjia
 * @description
 * @date 2021/4/1 15:50:05
 */
@WebFilter
public class LoginFilter implements Filter {

    private static final String USERNAME = "username";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("...");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String username = CookieUtils.getCookieValue(request,USERNAME);

        //未登录重定向到登录页面
        if (StringUtils.isEmpty(username)) {
            response.sendRedirect("/login");
        }

        filterChain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}

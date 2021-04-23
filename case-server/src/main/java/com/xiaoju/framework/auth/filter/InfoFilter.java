package com.xiaoju.framework.auth.filter;

import com.xiaoju.framework.auth.filter.request.RequestHeaderWrapper;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 该过滤器用于将cookie中的username取出并放入请求头中。
 *
 * @author littleforestjia
 * @description
 * @date 2021/4/1 15:50:05
 */
@WebFilter
public class InfoFilter implements Filter {

    //设置登录页不用进行过滤的资源路径
    private static final Set<String> UNALLOWPATH = Collections.unmodifiableSet(new HashSet<>(
        Arrays.asList("/api/user/login","/api/user/register","/login")));

    private static final String QUITPATH = "/api/user/quit";

    private static final String USERNAME = "username";

//    private static final String CHANNEL = "channel";
//
//    private static final String LINEID = "lineId";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("...");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        RequestHeaderWrapper wrapper = new RequestHeaderWrapper(request);

//        //如果请求头中没有username，则添加值为visitor的username请求头。
//        String username = wrapper.getHeader(USERNAME);
//        if (StringUtils.isEmpty(username)) {
//            wrapper.addHeader(USERNAME,"visitor");
//        }
//
//        wrapper.addHeader(CHANNEL,"1");
//        wrapper.addHeader(LINEID,"1");


        //获取资源路径
        String path = request.getRequestURI().substring(request.getContextPath().length()).replaceAll("[/]+$", "");

        //如果是登录或者注册操作则跳过过滤操作
        if (UNALLOWPATH.contains(path)) {
            filterChain.doFilter(wrapper,response);
            return;
        }

//        //如果是用户退出则将对应请求头删除
//        if (QUITPATH.equals(path)) {
//            wrapper.addHeader(USERNAME,null);
//            filterChain.doFilter(wrapper,response);
//            return;
//        }

        //寻找Cookie中键为username的数据，并将其添加到请求头中。
        String username = CookieUtils.getCookieValue(request,USERNAME);
        if (!StringUtils.isEmpty(username)) {
            wrapper.addHeader(USERNAME,username);
        }else {

            //1.未登录重定向到登录页面
            //response.sendRedirect("/login");

            //2.未登录则报错
            //throw new CaseServerException("用户未登录", StatusCode.AUTH_UNLOGIN);
        }

        filterChain.doFilter(wrapper,response);
    }

    @Override
    public void destroy() {

    }
}

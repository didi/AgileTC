package com.xiaoju.framework.filter;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.Authority;
import com.xiaoju.framework.service.UserService;
import com.xiaoju.framework.util.CookieUtils;
import org.apache.tomcat.websocket.server.WsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Component
public class WebSocketFilter extends FilterRegistrationBean<WsFilter> {

    @Resource
    private UserService userService;

    @Value("${authority.flag}")
    private Boolean authorityFlag;

    @PostConstruct
    public void init() {

        setFilter(new WsAuthFilter());
        setUrlPatterns(Arrays.asList("/api/dir/*", "/api/backup/*", "/api/record/*", "/api/file/*", "/api/case/*"));
    }

    class WsAuthFilter extends WsFilter {
        final Logger LOGGER = LoggerFactory.getLogger(WsAuthFilter.class);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest)request;
            try {
                authenticateByCookie(req);
                chain.doFilter(request, response);
            } catch (SecurityException e) {
                LOGGER.error("认证失败。", e);
                JSONObject o = new JSONObject();
                o.put("code", StatusCode.AUTHORITY_ERROR);
                o.put("msg", StatusCode.AUTHORITY_ERROR.getMsg());
                OutputStream out = response.getOutputStream();
                out.write(o.toJSONString().getBytes("UTF-8"));
                out.flush();
            }
        }

        private void authenticateByCookie(HttpServletRequest req) {
            if (!authorityFlag) return;
            String username = CookieUtils.getCookieValue(req, "username");
            // 获取用户对应权限的路径匹配列表
            List<String> authorityContent = userService.getUserAuthorityContent(username);

            String pathInfo = req.getPathInfo();
            String path;
            if (pathInfo == null) {
                path = req.getServletPath();
            } else {
                path = req.getServletPath() + pathInfo;
            }

            AntPathMatcher antPathMatcher = new AntPathMatcher();
            for (String auth: authorityContent) {
                boolean bRet = antPathMatcher.match(auth, path);
                if (bRet) {
                    LOGGER.info("权限认证成功, auth:" + auth + ", path: " + path);
                    return;
                }
            }
            LOGGER.info("权限认证失败, request path: " + path + "，username: " + username);
            throw new SecurityException("认证失败");
        }
    }
}

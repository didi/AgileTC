package com.xiaoju.framework.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by didi on 2021/4/22.
 */
public class CookieUtils {
    static final Logger logger = LoggerFactory.getLogger(CookieUtils.class);

    /**
     * 得到Cookie的值, 不编码
     *
     * @param request
     * @param cookieName
     * @return
     */
    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        return getCookieValue(request, cookieName, false);
    }

    /**
     * 得到Cookie的值,
     *
     * @param request
     * @param cookieName
     * @return
     */
    public static String getCookieValue(HttpServletRequest request, String cookieName, boolean isDecoder) {
        Cookie[] cookieList = request.getCookies();
        if (cookieList == null || cookieName == null){
            return null;
        }
        String retValue = null;
        try {
            for (int i = 0; i < cookieList.length; i++) {
                if (cookieList[i].getName().equals(cookieName)) {
                    if (isDecoder) {
                        retValue = URLDecoder.decode(cookieList[i].getValue(), "UTF-8");
                    } else {
                        retValue = cookieList[i].getValue();
                    }
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Cookie Decode Error.", e);
        }
        return retValue;
    }

    /**
     * 得到Cookie的值,
     *
     * @param request
     * @param cookieName
     * @return
     */
    public static String getCookieValue(HttpServletRequest request, String cookieName, String encodeString) {
        Cookie[] cookieList = request.getCookies();
        if (cookieList == null || cookieName == null){
            return null;
        }
        String retValue = null;
        try {
            for (int i = 0; i < cookieList.length; i++) {
                if (cookieList[i].getName().equals(cookieName)) {
                    retValue = URLDecoder.decode(cookieList[i].getValue(), encodeString);
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Cookie Decode Error.", e);
        }
        return retValue;
    }

    /**
     * 生成cookie，并指定编码
     * @param request 请求
     * @param response 响应
     * @param cookieName name
     * @param cookieValue value
     * @param encodeString 编码
     */
    public static final void setCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue, String encodeString) {
        setCookie(request,response,cookieName,cookieValue,null,encodeString, null);
    }

    /**
     * 生成cookie，并指定生存时间
     * @param request 请求
     * @param response 响应
     * @param cookieName name
     * @param cookieValue value
     * @param cookieMaxAge 生存时间
     */
    public static final void setCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue, Integer cookieMaxAge) {
        setCookie(request,response,cookieName,cookieValue,cookieMaxAge,null, null);
    }

    /**
     * 设置cookie，不指定httpOnly属性
     */
    public static final void setCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue, Integer cookieMaxAge, String encodeString) {
        setCookie(request,response,cookieName,cookieValue,cookieMaxAge,encodeString, null);
    }

    /**
     * 设置Cookie的值，并使其在指定时间内生效
     *
     * @param cookieMaxAge
     *            cookie生效的最大秒数
     */
    public static final void setCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue, Integer cookieMaxAge, String encodeString, Boolean httpOnly) {
        try {
            if(StringUtils.isBlank(encodeString)) {
                encodeString = "utf-8";
            }

            if (cookieValue == null) {
                cookieValue = "";
            } else {
                cookieValue = URLEncoder.encode(cookieValue, encodeString);
            }
            Cookie cookie = new Cookie(cookieName, cookieValue);
            if (cookieMaxAge != null && cookieMaxAge > 0)
                cookie.setMaxAge(cookieMaxAge);

            cookie.setPath("/");

            if(httpOnly != null) {
                cookie.setHttpOnly(httpOnly);
            }
            response.addCookie(cookie);
        } catch (Exception e) {
            logger.error("Cookie Encode Error.", e);
        }
    }

    /**
     * 得到cookie的域名
     */
    private static final String getDomainName(HttpServletRequest request) {
        String domainName = null;

        String serverName = request.getRequestURL().toString();
        if (serverName == null || serverName.equals("")) {
            domainName = "";
        } else {
            serverName = serverName.toLowerCase();
            serverName = serverName.substring(7);
            final int end = serverName.indexOf("/");
            serverName = serverName.substring(0, end);
            final String[] domains = serverName.split("\\.");
            int len = domains.length;
            if (len > 3) {
                // www.xxx.com.cn
                domainName = domains[len - 3] + "." + domains[len - 2] + "." + domains[len - 1];
            } else if (len <= 3 && len > 1) {
                // xxx.com or xxx.cn
                domainName = domains[len - 2] + "." + domains[len - 1];
            } else {
                domainName = serverName;
            }
        }

        if (domainName != null && domainName.indexOf(":") > 0) {
            String[] ary = domainName.split("\\:");
            domainName = ary[0];
        }
        return domainName;
    }
}

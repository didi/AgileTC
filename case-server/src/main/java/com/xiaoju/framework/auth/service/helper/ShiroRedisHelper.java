package com.xiaoju.framework.auth.service.helper;

import com.xiaoju.framework.auth.cache.AgileCacheManager;
import com.xiaoju.framework.auth.entity.AgileToken;
import com.xiaoju.framework.util.SpringUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author littleforestjia
 * @description
 * @date 2021/3/23 17:20:56
 */
public class ShiroRedisHelper {

    private static AgileCacheManager cacheManager = SpringUtils.getBean(AgileCacheManager.class);

    private static Logger LOGGER = LoggerFactory.getLogger(ShiroRedisHelper.class);

    /**
     * 清除指定用户的认证缓存和授权缓存
     */
    public static void delete(AgileToken token) {
        PrincipalCollection principals = new SimpleAuthenticationInfo(token, token, "AGILE-REALM").getPrincipals();
        deleteAuthorizationInfo(principals);
        deleteAuthenticationInfo(token);
    }

    /**
     * 清除指定用户的授权缓存
     */
    public static void deleteAuthorizationInfo(PrincipalCollection principals) {
        Cache<Object, AuthorizationInfo> cache = cacheManager.getCache("AGILE-REALM-AuthorizationCache");

        LOGGER.info("[Authorization_redis_delete]key:{}, timestamp:{}", principals.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));

        cache.remove(principals);
    }

    /**
     * 清除指定用户的认证缓存
     */
    public static void deleteAuthenticationInfo(AgileToken principal) {
        Cache<Object, AuthenticationInfo> cache = cacheManager.getCache("AGILE-REALM-AuthenticationCache");

        LOGGER.info("[Authentication_redis_delete]key:{}, timestamp:{}", principal.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));

        cache.remove(principal);
    }
}

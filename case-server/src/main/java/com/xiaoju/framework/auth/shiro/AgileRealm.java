package com.xiaoju.framework.auth.shiro;

import com.xiaoju.framework.auth.constants.AuthConstant;
import com.xiaoju.framework.auth.entity.AgileToken;
import com.xiaoju.framework.auth.entity.pojo.Permission;
import com.xiaoju.framework.auth.entity.pojo.User;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.mapper.UserMapper;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证+授权的内容
 *
 * @author didi
 * @date 2021/1/20
 */
@Component(value = "realm")
public class AgileRealm extends AuthorizingRealm implements Serializable {

    private static final long serialVersionUID = -2741710248922440453L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgileRealm.class);

    private static final String REALM_NAME = "AGILE-REALM";

    @Override
    public void setCacheManager(@Autowired CacheManager cacheManager) {
        super.setCacheManager(cacheManager);
        //认证缓存开启
        super.setAuthenticationCachingEnabled(true);
        //为认证缓存命名
        super.setAuthenticationCacheName("AGILE-REALM-AuthenticationCache");
        //授权缓存开启
        super.setAuthorizationCachingEnabled(true);
        //为授权缓存命名
        super.setAuthorizationCacheName("AGILE-REALM-AuthorizationCache");
    }

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof AgileToken;
    }

    /**
     * 认证操作
     *
     * @param token 用户登录后的身份信息
     * @return 认证后的基础信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        LOGGER.info("[Login]Username:{}, timestamp:{}", token.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));

        AgileToken principal = (AgileToken) token.getPrincipal();

        // 从数据库查信息,导出用户信息
        User user = userMapper.getUser(principal.getUsername(), principal.getChannel(), principal.getLineId());

        // 角色相关的校验
        if (user == null) {

            //如果数据库中没有这个用户，则报错。
            throw new CaseServerException("该用户不存在",StatusCode.AUTH_UNKNOWN);

        } else if (AuthConstant.BLOCKED.equals(user.getIsBlock())) {
            throw new CaseServerException(StatusCode.AUTH_BLOCKED);
        }

        // 给用户设置默认的信息,由于我们不需要管理用户登录态,所以只需要塞入即可
        // 就是表示该用户认证通过，返回认证后取得认证令牌的用户。
        return new SimpleAuthenticationInfo(principal, principal, REALM_NAME);
    }

    /**
     * 授权操作
     *
     * @param principalCollection 认证后的信息集合
     * @return 授权信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        AgileToken principal = (AgileToken) principalCollection.getPrimaryPrincipal();

        // 从数据库查询信息,导出权限相关的信息
        // 就是从数据库中通过用户、角色、权限三个表查出该用户对应的权限信息
        User user = userMapper.getUserWithPerms(principal.getUsername(), principal.getChannel(), principal.getLineId());

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        if (user == null) {
            throw new CaseServerException("您没有权限进行此操作", StatusCode.AUTH_ERROR);
        } else if (CollectionUtils.isEmpty(user.getPermissions())) {
            throw new CaseServerException("你没有权限进行此操作", StatusCode.AUTH_ERROR);
        } else {
            List<String> perms = user.getPermissions().stream().map(Permission::getResource).collect(Collectors.toList());

            //为该用户进行输入参数用户进行权限授权，将perms列表的权限授权给该用户；
            //因为本项目没有使用角色校验，所以不用为用户进行角色授权。
            info.addStringPermissions(perms);
        }

        return info;
    }
}

package com.xiaoju.framework.auth.config;

import com.xiaoju.framework.auth.filter.AuthFilter;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * shiro认证相关的config
 *
 * @author didi
 * @date 2021/1/20
 */
@Configuration
public class ShiroConfig {

    /**
     * web容器-请求拦截器
     * /agile相关的请求视作业务操作
     * /settings相关的请求视作设置相关
     * 拦截相关的内容请看 {@code AuthFilter.class}
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        Map<String, Filter> filterMap = new LinkedHashMap<>();
        filterMap.put("header", new AuthFilter());

        Map<String, String> ruleMap = new LinkedHashMap<>();
        ruleMap.put("/api/**", "header");

        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        bean.setSecurityManager(securityManager);
        bean.setUnauthorizedUrl("/403");
        bean.setFilters(filterMap);
        bean.setFilterChainDefinitionMap(ruleMap);
        return bean;
    }

    /**
     * {@code RequiresPermissions.class}
     * {@code RequiresRoles.class}
     * 权限注解的解析+装饰器，装配到容器中
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * 安全管理器,同时将Realm塞入进来
     */
    @Bean(name = "securityManager")
    public DefaultWebSecurityManager securityManager(Realm realm, CacheManager cacheManager) {
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        manager.setRealm(realm);
        manager.setCacheManager(cacheManager);
        return manager;
    }

    /**
     * 用于支持shiro的注解生效
     * @see AuthorizationAttributeSourceAdvisor
     * 不注册这个会导致request不生效，走不到controller
     */
    @Bean
    public static DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }

}

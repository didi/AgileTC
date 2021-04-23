package com.xiaoju.framework.auth.cache;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @author hcy
 * @date 2021/2/18
 */
@Component
public class AgileCacheManager extends AbstractCacheManager {

    private HashMap cacheMap = new HashMap();

    @Override
    protected Cache createCache(String s) throws CacheException {
        return new AgileCache(cacheMap);
    }
}

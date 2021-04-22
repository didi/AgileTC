package com.xiaoju.framework.auth.cache;

import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * redis缓存的实现
 *
 * @author hcy
 * @date 2021/2/18
 */
public class AgileCache<K, V> implements Cache<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgileCache.class);

    private HashMap cacheMap;

    public AgileCache(HashMap cacheMap) {
        this.cacheMap = cacheMap;
    }

    @Override
    public V get(K k) throws CacheException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[Shiro Get]{}", k);
        }

        if (k == null) {
            return null;
        }

        Object res = cacheMap.get(k);
        if (res == null) {
            return null;
        }

        return (V) res;
    }

    @Override
    public V put(K k, V v) throws CacheException {
        if (k == null || v == null) {
            LOGGER.error("[Shiro Put]key={}, value={}", k, v);
            throw new CaseServerException("不能塞入空值", StatusCode.SERVER_BUSY_ERROR);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[Shiro Put]key={}, value={}", k, v);
        }
        cacheMap.put(k, v);
        return v;
    }

    @Override
    public V remove(K k) throws CacheException {
        if (k == null) {
            return null;
        }
        V res = get(k);
        cacheMap.remove(k);
        return res;
    }

    @Override
    public void clear() throws CacheException {
        // should do nothing
    }

    @Override
    public int size() {
        // should do nothing
        return 0;
    }

    @Override
    public Set<K> keys() {
        // should do nothing
        return new HashSet<>();
    }

    @Override
    public Collection<V> values() {
        // should do nothing
        return new ArrayList<>();
    }
}

package com.xiaoju.framework.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Repository;

/**
 * Spring相关函数
 *
 * @author didi
 * @date 2020/9/3
 */
@Repository
public final class SpringUtils implements BeanFactoryPostProcessor {

    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringUtils.beanFactory = beanFactory;
    }

    public static ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * 根据名称获取对象
     *
     * @param name bean的名称
     * @return Object 一个以所给名字注册的bean的实例
     * @throws org.springframework.beans.BeansException bean没找到的异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T) getBeanFactory().getBean(name);
    }

    /**
     * 根据类型获取对象
     *
     * @param clz 类型
     * @return 对象实体
     * @throws org.springframework.beans.BeansException bean没找到的异常
     *
     */
    public static <T> T getBean(Class<T> clz) throws BeansException {
        return getBeanFactory().getBean(clz);
    }

    /**
     * 根据名称，查看工厂中是否含有此bean
     *
     * @param name bean名称
     * @return boolean
     */
    public static boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

}
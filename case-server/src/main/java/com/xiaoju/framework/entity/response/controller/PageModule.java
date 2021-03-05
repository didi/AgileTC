package com.xiaoju.framework.entity.response.controller;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页
 *
 * @author didi
 * @date 2020/7/30
 */
@Data
public class PageModule<T> implements Serializable {

    private static final long serialVersionUID = -3504431894726195820L;

    private List<T> dataSources;

    private Long total;

    public static <T> PageModule<T> buildPage(List<T> dataSource, Long total) {
        PageModule<T> obj = new PageModule<>();
        obj.setDataSources(dataSource);
        obj.setTotal(total);
        return obj;
    }

    public static <T> PageModule<T> emptyPage() {
        PageModule<T> obj = new PageModule<>();
        obj.setDataSources(new ArrayList<>());
        obj.setTotal(0L);
        return obj;
    }
}

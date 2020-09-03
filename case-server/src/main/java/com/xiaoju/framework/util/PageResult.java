package com.xiaoju.framework.util;

import java.io.Serializable;
import java.util.List;

/**
 * Created by didi on 2019/9/24.
 */
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Number total = 0;
    private List<T> data = null;

    public PageResult() {
    }

    public PageResult(List<T> data) {
        this.data = data;
    }

    public PageResult(List<T> data, Number total) {
        this.data = data;
        this.total = total;
    }

    /**
     * Getter method for property <tt>total</tt>.
     *
     * @return property value of total
     */
    public Number getTotal() {
        return total;
    }

    /**
     * Setter method for property <tt>total</tt>.
     *
     * @param total value to be assigned to property total
     */
    public void setTotal(Number total) {
        this.total = total;
    }

    /**
     * Getter method for property <tt>data</tt>.
     *
     * @return property value of data
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Setter method for property <tt>data</tt>.
     *
     * @param data value to be assigned to property data
     */
    public void setData(List<T> data) {
        this.data = data;
    }
}

package com.xiaoju.framework.entity.persistent;

import lombok.Data;

import java.util.Date;

/**
 * 文件夹
 *
 * 一个业务线有一条自己的文件夹数据，这里采用json去存储
 *
 * @author didi
 * @date 2020/09/09
 */
@Data
public class Biz {

    /**
     * id
     */
    private Long id;

    /**
     * 业务线id
     */
    private Long productLineId;

    /**
     * channel 当前默认1
     */
    private Integer channel;

    private Integer isDelete;

    private Date gmtModified;

    private Date gmtCreated;

    /**
     * 存储的内容
     */
    private String content;
}

package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/14 14:00:59
 */
@Data
public class UserListReq {
    private String username;

    private Long roleId;

    private String beginTime;

    private String endTime;

    private Integer pageNum;

    private Integer pageSize;

    public UserListReq(String username,Long roleId,String beginTime,String endTime,Integer pageNum,Integer pageSize) {
        this.username = username;
        this.roleId = roleId;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}

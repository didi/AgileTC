package com.xiaoju.framework.auth.entity.pojo;

import lombok.Data;

import java.util.Date;

/**
 * 角色和权限的关联
 *
 * @author didi
 * @date 2021/1/21
 */
@Data
public class RolePermRel {

    private Long id;

    private Long permId;

    private Long roleId;

    private Integer isDelete;

    private String creator;

    private String modifier;

    private Date createTime;

    private Date updateTime;
}

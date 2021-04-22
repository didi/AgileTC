package com.xiaoju.framework.auth.entity.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 角色，可以由用户自由修改
 *
 * @author didi
 * @date 2021/1/21
 */
@Data
public class Role {

    private Long id;

    private String roleName;

    private Integer isDelete;

    private Integer isBlock;

    private Integer type;

    private Long lineId;

    private Integer channel;

    private String creator;

    private String modifier;

    private Date createTime;

    private Date updateTime;

    private List<Permission> permissions;

    private List<User> users;
}

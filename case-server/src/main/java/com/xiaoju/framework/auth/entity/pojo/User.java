package com.xiaoju.framework.auth.entity.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用户信息，用户只能添加，理论上不能删除
 *
 * @author didi
 * @date 2021/1/21
 */
@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String salt;

    private Integer isBlock;

    private Integer isDelete;

    private Long lineId;

    private Integer channel;

    private Date createTime;

    private Date updateTime;

    /**
     * 非映射字段
     * 某个用户的权限集合
     */
    private List<Permission> permissions;

    /**
     * 某个用户的角色
     */
    private Role role;

    /**
     * 获取用户的三要素字符串
     * @param user
     * @return
     */
    public static String buildUserKey(User user) {
        StringBuilder builder = new StringBuilder();
        builder.append(user.getUsername()).append(",")
                .append(user.getChannel()).append(",")
                .append(user.getLineId());
        return builder.toString();
    }
}

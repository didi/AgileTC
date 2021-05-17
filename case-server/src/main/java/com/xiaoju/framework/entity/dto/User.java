package com.xiaoju.framework.entity.dto;

import lombok.Data;

import java.util.Date;

/**
 * Created by didi on 2021/4/22.
 */
@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String salt;

    private String authorityName;

    private Integer isDelete;

    private Integer channel;

    private Long productLineId;

    private Date gmtCreated;

    private Date gmtUpdated;

    /**
     * 获取用户的三要素字符串
     * @param user
     * @return
     */
    public static String buildUserKey(User user) {
        StringBuilder builder = new StringBuilder();
        builder.append(user.getUsername()).append(",")
                .append(user.getChannel()).append(",")
                .append(user.getProductLineId());
        return builder.toString();
    }
}

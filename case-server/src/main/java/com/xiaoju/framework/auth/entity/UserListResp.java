package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/14 14:02:04
 */
@Data
public class UserListResp {
    private Long userId;

    private Long roleId;

    private String username;

    private String roleName;
}

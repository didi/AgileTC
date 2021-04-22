package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/19 16:20:26
 */
@Data
public class UserRoleUpdateReq {
    private Long userId;

    private Long oldRoleId;

    private Long newRoleId;
}

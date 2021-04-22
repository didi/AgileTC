package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/13 14:19:16
 */
@Data
public class UserRegisterReq {
    private String username;

    private String password;
}

package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * @author littleforestjia
 * @description
 * @date 2021/4/13 14:33:13
 */
@Data
public class UserLoginReq {
    private String username;

    private String password;
}

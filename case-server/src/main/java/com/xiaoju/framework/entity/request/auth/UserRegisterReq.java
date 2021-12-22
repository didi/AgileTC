package com.xiaoju.framework.entity.request.auth;

import lombok.Data;

/**
 * Created by didi on 2021/4/22.
 */
@Data
public class UserRegisterReq {
    private String username;

    private String password;
}

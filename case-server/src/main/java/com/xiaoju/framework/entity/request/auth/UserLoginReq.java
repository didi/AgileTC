package com.xiaoju.framework.entity.request.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by didi on 2021/4/22.
 */
@Data
public class UserLoginReq {
    private String username;

    private String password;
}

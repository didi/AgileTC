package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * 角色删除
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
public class RoleDeleteReq implements ParamValidate {

    private Long roleId;

    private String username;

    private Long lineId;

    private Integer channel;

    @Override
    public void validate() {

    }
}

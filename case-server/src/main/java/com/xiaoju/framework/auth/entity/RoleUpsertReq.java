package com.xiaoju.framework.auth.entity;

import lombok.Data;

/**
 * TODO
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
public class RoleUpsertReq implements ParamValidate {

    private Long roleId;

    private String roleName;

    private String permIds;

    private String users;

    private String username;

    private Long lineId;

    private Integer channel;

    @Override
    public void validate() {

    }
}

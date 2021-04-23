package com.xiaoju.framework.auth.entity.pojo;

import lombok.Data;

/**
 * 权限，这部分由后端写死，随着功能增加而增加
 * 不可以由用户修改
 *
 * @author didi
 * @date 2021/1/21
 */
@Data
public class Permission {

    private Long id;

    private String permName;

    private String resource;

    private Integer isDelete;

}

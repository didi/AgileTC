package com.xiaoju.framework.auth.entity;

import com.xiaoju.framework.auth.entity.pojo.Permission;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限输出体
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
public class PermResp {

    private Long permId;

    private String permName;

    public static PermResp buildPermResp(Permission permission) {
        PermResp resp = new PermResp();
        if (permission != null) {
            resp.setPermId(permission.getId());
            resp.setPermName(permission.getPermName());
        }
        return resp;
    }

    public static List<PermResp> buildMultiResp(List<Permission> permissionList) {
        // <所有操作权限>不会被扔出去
        return permissionList.stream()
                .filter(p -> !p.getResource().equals("*:*"))
                .map(PermResp::buildPermResp)
                .collect(Collectors.toList());
    }
}

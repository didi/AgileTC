package com.xiaoju.framework.auth.entity.pojo;

import lombok.Data;

import java.util.Date;

import static com.xiaoju.framework.auth.constants.AuthConstant.SYSTEM;
import static com.xiaoju.framework.constants.SystemConstant.NOT_DELETE;

/**
 * 用户和角色的关联
 *
 * @author didi
 * @date 2021/1/21
 */
@Data
public class UserRoleRel {

    private Long id;

    private Long userId;

    private Long roleId;

    private Integer isDelete;

    private String creator;

    private String modifier;

    private Integer channel;

    private Long lineId;

    private Date createTime;

    private Date updateTime;

    public static UserRoleRel buildRel(User user, Role role) {
        UserRoleRel rel = new UserRoleRel();
        rel.setUserId(user.getId());
        rel.setRoleId(role.getId());
        rel.setIsDelete(NOT_DELETE);
        rel.setCreator(SYSTEM);
        rel.setModifier(SYSTEM);
        rel.setLineId(user.getLineId());
        rel.setChannel(user.getChannel());
        rel.setCreateTime(new Date());
        rel.setUpdateTime(new Date());
        return rel;
    }
}

package com.xiaoju.framework.auth.entity;

import com.xiaoju.framework.auth.entity.pojo.Role;
import com.xiaoju.framework.entity.response.PersonResp;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户与角色的关联
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
public class RoleListResp {

    private Long roleId;

    private String roleName;

    private List<PersonResp> users = new ArrayList<>();

    private boolean canDelete;

    public static RoleListResp buildSingleResp(Role role) {
        RoleListResp resp = new RoleListResp();
        resp.setRoleId(role.getId());
        resp.setRoleName(role.getRoleName());
        resp.setUsers(role.getUsers().stream().map(PersonResp::buildPersonResp).collect(Collectors.toList()));
        resp.setCanDelete(role.getType() <= 0);
        return resp;
    }

    public static List<RoleListResp> buildMultiResp(List<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return Lists.newArrayList();
        }

        //取出1\2\3\4基础角色放在列表的前四位。
        HashMap<Integer,Role> map = new HashMap<>();
        Deque<RoleListResp> deque = new LinkedList<>();
        for (int i = 0; i < roles.size(); i++) {
            if(roles.get(i).getType() > 0) {
                map.put(roles.get(i).getType(),roles.get(i));
            }else {
                deque.addLast(RoleListResp.buildSingleResp(roles.get(i)));
            }
        }
        for (int i = 1; i <= 4; i++) {
            if (map.containsKey(i)) {
                deque.addFirst(RoleListResp.buildSingleResp(map.get(i)));
            }
        }

        return new ArrayList<>(deque);
    }
}

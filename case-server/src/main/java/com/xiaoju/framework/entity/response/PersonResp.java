package com.xiaoju.framework.entity.response;

import com.xiaoju.framework.auth.entity.pojo.User;
import lombok.Data;

/**
 * 返回的人员
 *
 * @author didi
 * @date 2020/11/24
 */
@Data
public class PersonResp {

    /**
     * 前缀
     */
    private String staffNamePY;

    /**
     * 中文名
     */
    private String staffNameCN;

    /**
     * 该方法主要给用户和权限系统使用，用于创建用户名对象
     * @param user
     * @return
     */
    public static PersonResp buildPersonResp(User user) {
        PersonResp resp = new PersonResp();
        resp.setStaffNamePY(user.getUsername());
        return resp;
    }
}

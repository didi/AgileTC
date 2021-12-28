package com.xiaoju.framework.entity.response;

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
}

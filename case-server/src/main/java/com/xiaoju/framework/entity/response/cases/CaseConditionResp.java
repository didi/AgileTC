package com.xiaoju.framework.entity.response.cases;

import lombok.Data;

import java.util.Set;

/**
 * 编辑页面-条件筛选后的结果内容
 *
 * @author hcy
 * @date 2020/10/21
 */
@Data
public class CaseConditionResp {

    /**
     * 符合条件的用例数
     */
    private Integer count;

    /**
     * 所有用例数
     */
    private Integer totalCount;

    /**
     * 有哪些resource标签
     */
    private Set<String> taglist;

}

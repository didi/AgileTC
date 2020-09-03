package com.xiaoju.framework.entity.Response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * @author ldl
 * @version: 1.0.0
 * @description:
 * @date 2020/7/20 3:57 下午
 */
@Data
public class ConditionTestCaseInfoVO {

    @ApiModelProperty("当前条件用例数")
    private Integer count;

    @ApiModelProperty("当前用例集的所有用例数")
    private Integer totalCount;

    @ApiModelProperty("标签集合")
    private Set<String> taglist;

}

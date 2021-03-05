package com.xiaoju.framework.entity.response.cases;

import lombok.Data;

/**
 * 脑图-查看用例上方的概览信息(不包括content)
 *
 * @author hcy
 * @date 2020/10/22
 */
@Data
public class CaseGeneralInfoResp {

    private Long id;

    private String title;

    private String requirementId;

    private Long productLineId;
}

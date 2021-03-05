package com.xiaoju.framework.entity.response.cases;

import com.xiaoju.framework.entity.response.dir.BizListResp;
import lombok.Data;

import java.util.List;

/**
 * 用例详情
 *
 * @author hcy
 * @date 2020/9/7
 */
@Data
public class CaseDetailResp {

    private Integer caseType;

    private String description;

    private Long id;

    private String modifier;

    private String requirementId;

    private String title;

    private Long productLineId;

    private List<BizListResp> biz;

    private Long groupId;

}

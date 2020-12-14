package com.xiaoju.framework.entity.request.cases;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 编辑页面-根据条件筛选用例中的条目
 *
 * @author didi
 * @date 2020/10/21
 */
@Data
public class CaseConditionReq implements ParamValidate {

    /**
     * 要针对搜索的用例id
     */
    private Long caseId;

    /**
     * 想要框选的优先级
     */
    private List<String> priority;

    /**
     * 想要框选的自定义标签
     * 如果用例内部没有标签，这里就不会有东西
     */
    private List<String> resource;

    @Override
    public void validate() {
        if (caseId == null || caseId <= 0) {
            throw new IllegalArgumentException("用例id为空或者非法");
        }

    }

    public CaseConditionReq(Long caseId, String[] priority, String[] resource) {
        this.caseId = caseId;
        if (priority == null) {
            throw new IllegalArgumentException("圈选优先级为空");
        }
        if (resource == null) {
            throw new IllegalArgumentException("圈选资源为空");
        }
        this.priority = Arrays.stream(priority).collect(Collectors.toList());
        this.resource = Arrays.stream(resource).collect(Collectors.toList());;
    }
}

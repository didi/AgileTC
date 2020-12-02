package com.xiaoju.framework.entity.request.record;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 任务 编辑
 *
 * @author hcy
 * @date 2020/10/29
 */
@Data
public class RecordUpdateReq implements ParamValidate {

    private Long id;

    private String modifier;

    private String owner;

    private String title;

    private String chooseContent;

    private String description;

    private Long expectEndTime;

    private Long expectStartTime;

    @Override
    public void validate() {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("任务id为空或不正确");
        }
        if (StringUtils.isEmpty(modifier)) {
            throw new IllegalArgumentException("修改人为空");
        }
        if (StringUtils.isEmpty(title)) {
            throw new IllegalArgumentException("任务标题为空");
        }
        if (expectStartTime != null && expectEndTime == null || expectStartTime == null && expectEndTime != null) {
            throw new IllegalArgumentException("计划周期时间区域不完整");
        }
        if (StringUtils.isEmpty(chooseContent)) {
            throw new IllegalArgumentException("圈选用例为空");
        }
    }
}

package com.xiaoju.framework.entity.request.record;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务 删除
 *
 * @author hcy
 * @date 2020/10/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDeleteReq implements ParamValidate {

    private Long id;

    @Override
    public void validate() {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("任务id为空或不正确");
        }
    }
}

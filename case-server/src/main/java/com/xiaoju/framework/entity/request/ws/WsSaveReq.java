package com.xiaoju.framework.entity.request.ws;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 脑图页面保存用例/任务的请求体
 *
 * @author didi
 * @date 2020/11/25
 */
@Data
public class WsSaveReq implements ParamValidate {

    /**
     * 传进来的是当前脑图的caseContent
     * 可能是record的也可能是testcase的
     */
    private String caseContent;

    /**
     * 改动前的内容。可能是 record 的，也可能是 testcase 的。若提供，会进行增量保存。若不提供，进行全量保存
     */
    private String baseCaseContent;

    private Long id;

    private String modifier;

    /**
     * 如果是用例页面 则传进来是null
     * 如果是任务页面 则传进来具体的任务id
     */
    private Long recordId;

    /**
     * 保存理由。若不为空，会作为历史记录的 title
     */
    private String saveReason;

    @Override
    public void validate() {
        if (StringUtils.isEmpty(caseContent)) {
            throw new IllegalArgumentException("保存的内容为空");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用例id为空");
        }
        if (StringUtils.isEmpty(modifier)) {
            throw new IllegalArgumentException("修改人为空");
        }
    }
}

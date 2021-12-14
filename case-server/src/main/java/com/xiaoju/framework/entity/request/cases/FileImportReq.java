package com.xiaoju.framework.entity.request.cases;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用例 导入文件并上传
 *
 * @author hcy
 * @date 2020/10/22
 */
@Data
@AllArgsConstructor
public class FileImportReq implements ParamValidate {

    private MultipartFile file;

    private String creator;

    private Long productLineId;

    private String title;

    private String description;

    /**
     * 默认为1，如果有其他需求，可以变为其他数字
     */
    private Integer channel;

    private String requirementId;

    private String bizId;

    @Override
    public void validate() {
        if (channel == null || channel < 0) {
            throw new IllegalArgumentException("渠道为空或者非法");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先上传文件");
        }
        if (productLineId == null || productLineId < 0L) {
            throw new IllegalArgumentException("业务线id为空或者非法");
        }
        if (StringUtils.isEmpty(title)) {
            throw new IllegalArgumentException("标题为空");
        }
        if (StringUtils.isEmpty(creator)) {
            throw new IllegalArgumentException("创建人为空");
        }
        if (StringUtils.isEmpty(bizId)) {
            // 特殊点，没有传bizId就给-1，而不是报错
            throw new IllegalArgumentException("文件夹id");
        }
    }
}

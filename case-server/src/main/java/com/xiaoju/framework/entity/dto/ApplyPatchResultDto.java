package com.xiaoju.framework.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplyPatchResultDto {

    /**
     * 应用了不冲突patch后的json
     */
    String jsonAfterPatch;

    /**
     * 存在冲突无法应用的 patch
     */
    List<String> conflictPatch;

    /**
     * 无冲突已应用的 patch
     */
    List<String> applyPatch;
}

package com.xiaoju.framework.entity.dto;

import com.xiaoju.framework.service.impl.RecordServiceImpl;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件夹节点转换体
 *
 * @author didi
 * @date 2020/10/28
 * @see RecordServiceImpl#getData(com.xiaoju.framework.entity.dto.MergeCaseDto)
 */
@Data
public class DirNodeDto {

    private String id;
    private String text;
    private String parentId;
    private Set<String> caseIds = new HashSet<>();

    private List<DirNodeDto> children = new ArrayList<>();
}

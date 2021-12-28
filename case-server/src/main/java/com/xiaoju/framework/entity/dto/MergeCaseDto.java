package com.xiaoju.framework.entity.dto;

import com.xiaoju.framework.service.impl.RecordServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 给getData专用的转换体
 *
 * @author hcy
 * @date 2020/10/28
 * @see RecordServiceImpl#getData(com.xiaoju.framework.entity.dto.MergeCaseDto)
 */
@Data
@AllArgsConstructor
public class MergeCaseDto {

    private Long caseId;

    private String chooseContent;

    private String recordContent;

    private Integer env;

    private Long recordId;
}

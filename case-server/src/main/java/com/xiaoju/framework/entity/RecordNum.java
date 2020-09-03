package com.xiaoju.framework.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RecordNum {

    Long caseId;

    Integer recordNum;

}

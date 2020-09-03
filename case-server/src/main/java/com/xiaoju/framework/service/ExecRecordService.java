package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.ExecRecord;
import com.xiaoju.framework.util.PageResult;

import java.util.List;

/**
 * Created by didi on 2019/9/23.
 */
public interface ExecRecordService {
    List<ExecRecord> getRecordByCaseId(Long caseId);

    PageResult<ExecRecord> getRecordByRequirementId(Long requirementId,int pageNum,int pageSize,int channel);

    ExecRecord getRecordById(Long id);

    ExecRecord addRecord(ExecRecord execRecord);

    List<Integer> getEnv(Long caseId,String creator);

    ExecRecord modifyTestRecord(ExecRecord execRecord);

    void deleteRecord(Long caseId);

    ExecRecord clearRecord(ExecRecord execRecord);

    int deleteRecordById(Long id);

    int editRecord(ExecRecord execRecord);

}

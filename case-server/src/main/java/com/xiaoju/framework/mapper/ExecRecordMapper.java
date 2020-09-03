package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.ExecRecord;
import com.xiaoju.framework.entity.RecordNum;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by didi on 2019/9/29.
 */
@Repository
public interface ExecRecordMapper {
    ExecRecord selectByPrimaryKey(Long id);

    void deleteByCaseId(Long caseId);

    Long insert(ExecRecord record);

    void updateById(ExecRecord record);
    void updateContentById(ExecRecord record);
    List<ExecRecord> listRecordByCaseId(Long caseId);

    List<ExecRecord> getEnvList(ExecRecord record);

    int deleteById(Long id);

    int editRecord(ExecRecord record);

    List<ExecRecord> selectByRequirementId(@Param("requirementId") String requirementId,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize,
                                           @Param("channel") int channel);
    int selectByRequirementTotal(@Param("requirementId") String requirementId,@Param("channel") int channel);

    List<RecordNum> getRecordNumByCaseIds(List<Long> caseIds);
}


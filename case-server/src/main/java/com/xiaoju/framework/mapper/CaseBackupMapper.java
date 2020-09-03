package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.CaseBackup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


/**
 * Created by didi on 2019/11/5.
 */
@Repository
public interface CaseBackupMapper {
    @Insert(value = "insert into case_backup values (null,#{caseId},#{title},#{creator},"
            + "CURRENT_TIMESTAMP,#{caseContent},#{recordContent},extra,0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCaseBackup(CaseBackup caseBackup);

    List<CaseBackup> selectByCaseId(@Param("caseId") Long caseId,
                                    @Param("beginTime") Date beginTime,
                                    @Param("endTime")  Date endTime);
    int updateByCaseId(Long caseId);
    int insert(CaseBackup caseBackup);
}

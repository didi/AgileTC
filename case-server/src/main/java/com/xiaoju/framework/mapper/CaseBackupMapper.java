package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.persistent.CaseBackup;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


/**
 * 备份映射
 *
 * @author didi
 * @date 2020/11/5
 * @see CaseBackup
 */
@Repository
public interface CaseBackupMapper {

    /**
     * 获取一份用例下所有的用例备份记录
     *
     * @param caseId 用例id
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 所有备份记录
     */
    List<CaseBackup> selectByCaseId(@Param("caseId") Long caseId,
                                    @Param("beginTime") Date beginTime,
                                    @Param("endTime")  Date endTime);

    CaseBackup selectByBackupId(@Param("id") Long id);

    /**
     * 删除一批备份记录
     *
     * @param caseId 用例id
     * @return int
     * @see com.xiaoju.framework.service.impl.CaseBackupServiceImpl#deleteBackup(java.lang.Long)
     */
    int updateByCaseId(Long caseId);

    /**
     * 插入备份记录
     *
     * @param caseBackup 实体
     * @return int
     */
    int insert(CaseBackup caseBackup);
}

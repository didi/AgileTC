package com.xiaoju.framework.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoju.framework.entity.persistent.CaseBackup;

import java.util.List;

/**
 * 备份接口
 *
 * @author didi
 * @date 2020/11/5
 */
public interface CaseBackupService {

    /**
     * 插入备份记录,比较内容后插入
     *
     * @param caseBackup 备份实体
     * @return 实体
     */
    CaseBackup insertBackup(CaseBackup caseBackup);

    /**
     * 获取一段时间内，某个用例备份记录
     *
     * @param
     * @param caseId 用例id
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 实体
     */
    List<CaseBackup> getBackupByCaseId(Long caseId, String startTime, String endTime);

    /**
     * 删除备份记录
     *
     * @param caseId 用例id
     * @return int
     */
    int deleteBackup(Long caseId);

    JsonNode getCaseDiff(Long backupId1, Long backupId2);

    int insertEditInfo(CaseBackup caseBackup);
}

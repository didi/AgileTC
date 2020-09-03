package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.CaseBackup;

import java.util.List;

/**
 * Created by didi on 2019/11/5.
 */
public interface CaseBackupService {
    CaseBackup insertBackup(CaseBackup caseBackup);
    List<CaseBackup> getBackupByCaseId(Long caseId,String sTime,String eTime);
    int deleteBackup(Long caseId);
}

package com.xiaoju.framework.service.impl;

import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.util.TimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 备份记录
 *
 * @author didi
 * @date 2020/11/5
 */
@Service
public class CaseBackupServiceImpl implements CaseBackupService {

    @Resource
    private CaseBackupMapper caseBackupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaseBackup insertBackup(CaseBackup caseBackup) {
        int backupId = caseBackupMapper.insert(caseBackup);
        caseBackup.setCaseId((long) backupId);
        return caseBackup;
    }

    @Override
    public List<CaseBackup> getBackupByCaseId(Long caseId, String beginTime, String endTime) {
        return caseBackupMapper.selectByCaseId(caseId, transferTime(beginTime), transferTime(endTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBackup(Long caseId ) {
        return caseBackupMapper.updateByCaseId(caseId);
    }

    private Date transferTime(String time) {
        if (time == null) {
            return null;
        }
        return TimeUtil.transferStrToDateInSecond(time);
    }
}

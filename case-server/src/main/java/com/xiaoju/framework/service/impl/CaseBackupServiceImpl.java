package com.xiaoju.framework.service.impl;

import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseBackupServiceImpl.class);

    @Resource
    private CaseBackupMapper caseBackupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized CaseBackup insertBackup(CaseBackup caseBackup) {
        // 此处可以与最新的内容比对，如果一致，则不更新backup表，减少版本数量
        List<CaseBackup> caseBackups = caseBackupMapper.selectByCaseId(caseBackup.getCaseId(), null, null);

        // 如果当前已有，则直接返回
        // todo: 此处应该是比较json或者base字段信息，此处可能存在json字段位置不一致导致的字符串不一致问题。
        if (caseBackups.size() > 0 &&
                caseBackups.get(0).getCaseContent().equals(caseBackup.getCaseContent()) &&
                caseBackups.get(0).getRecordContent().equals(caseBackup.getRecordContent())) {
            LOGGER.info("当前内容已经保存过了，不再重复保存。");
            return caseBackups.get(0);
        }

        int ret = caseBackupMapper.insert(caseBackup);
        if (ret < 1) {
            LOGGER.error("用例备份落库失败. casebackup id: " + caseBackup.getCaseId() + ", case content: " +
                    caseBackup.getCaseContent() + ", record: " + caseBackup.getRecordContent());
            return null;
        }

        LOGGER.info("备份保存当前用例。caseid:" + caseBackup.getCaseId());

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

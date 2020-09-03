package com.xiaoju.framework.service.impl;

import com.xiaoju.framework.entity.CaseBackup;
import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.util.ErrorCode;
import com.xiaoju.framework.util.PageResult;
import com.xiaoju.framework.util.ResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by didi on 2019/11/5.
 */
@Slf4j
@Service
public class CaseBackupServiceImpl implements CaseBackupService {
    @Autowired
    private CaseBackupMapper caseBackupMapper;

    @Override
    public CaseBackup insertBackup(CaseBackup caseBackup) {

        //return caseBackupMapper.insertCaseBackup(caseBackup);
        try{
            if(caseBackup.getCaseId()==null){
                throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "caseId不可为空");
            }
            Integer backupId = caseBackupMapper.insert(caseBackup);
            caseBackup.setCaseId(backupId.longValue());
            return  caseBackup;
        }
        catch (ResponseException e){
            log.warn("case create failed. " + e.getMsg());
            throw new ResponseException(e.getErrorCode(), e.getMsg());
        }
    }

    @Override
    public List<CaseBackup> getBackupByCaseId(Long caseId, String beginTime, String endTime) {
        Date sTime = transferDate(beginTime), eTime =transferDate(endTime);
        List<CaseBackup> backupList = caseBackupMapper.selectByCaseId(caseId, sTime, eTime);
        return backupList;
    }

    @Override
    public int deleteBackup(Long caseId ) {
        try{
            if(caseId==null){
                throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "caseId不可为空");
            }
           return caseBackupMapper.updateByCaseId(caseId);
        }
        catch (ResponseException e){
            log.warn("case create failed. " + e.getMsg());
            throw new ResponseException(e.getErrorCode(), e.getMsg());
        }
    }

    private Date transferDate(String beforeTime)
    {
        Date afterTime = null;
        if (beforeTime != "" && beforeTime != null) {
            try {
                afterTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(beforeTime);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        return afterTime;
    }
}

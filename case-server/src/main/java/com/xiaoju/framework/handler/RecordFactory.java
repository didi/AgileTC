package com.xiaoju.framework.handler;

import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;

import java.util.concurrent.ConcurrentHashMap;

public class RecordFactory {
    static ConcurrentHashMap<String, RecordEntity> recordEntityMap = new ConcurrentHashMap<>();

    public static RecordEntity getRoom(String roomId, Long caseId, TestCaseMapper caseMapper, Long recordId, ExecRecordMapper recordMapper) {

        if (recordEntityMap.containsKey(roomId)) {
            return recordEntityMap.get(roomId);
        } else {
            RecordEntity roomEntity = new RecordEntity(roomId, caseId, caseMapper, recordId, recordMapper);
            recordEntityMap.put(roomId, roomEntity);
            return roomEntity;
        }
    }
}

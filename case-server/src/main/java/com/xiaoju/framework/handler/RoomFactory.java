package com.xiaoju.framework.handler;

import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;

import java.util.concurrent.ConcurrentHashMap;

public class RoomFactory {
    static ConcurrentHashMap<String, RoomEntity> roomEntityMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, RecordEntity> recordEntityMap = new ConcurrentHashMap<>();

    public static RoomEntity getRoom(String roomId, Long caseId, TestCaseMapper caseMapper) {
//        synchronized (roomId) { // todo：此处暂不确定是否有效，需要进行验证

            if (roomEntityMap.containsKey(roomId)) {
                return roomEntityMap.get(roomId);
            } else {
                RoomEntity roomEntity = new RoomEntity(roomId, caseId, caseMapper);
                roomEntityMap.put(roomId, roomEntity);
                return roomEntity;
            }

//        }
    }

    public static void clearRoom(String roomId) {
        roomEntityMap.remove(roomId);
    }
}

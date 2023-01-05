package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import java.util.concurrent.ExecutorService;

public class LockIngressTask extends IngressTask {
    PushMessage data;

    public LockIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService, PushMessage data) {
        super(client, socketIOServer, room, executorEgressService);
        this.data = data;
    }

    @Override
    public void run()  {
        LOGGER.info("websocket client:{}",JSON.toJSONString(client));
        room.lock();
        LOGGER.info(data.getMessage());
        ClientEntity clientEntity = getRoomFromClient(client);
        String roomId = clientEntity.getRoomId();

        if (data.getMessage().equals("lock")) { // lock消息
            if (room.isLockedByClient()) {
                client.sendEvent("lock", PushMessage.builder().message("3").build()); // 当前已经lock了,后续可以发送详细锁住人信息
                return;
            } else {
                room.clientLock(client);
            }
        } else if(data.getMessage().equals("unlock")) {
            if (!room.isLockedByClient()) { // 当前已经unlock状态
                client.sendEvent("lock", PushMessage.builder().message("3").build()); // 当前已经lock了,后续可以发送详细锁住人信息
                return;
            } else {
                if (room.lockByClient(client)) { // 自己锁的
                    room.clientUnlock();
                } else {// 其他人锁的
                    client.sendEvent("lock", PushMessage.builder().message("3").build()); // 当前已经lock了,后续可以发送详细锁住人信息
                    return;
                }
            }
        }
        BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);
        broadcastOperations.sendEvent("lock", client, PushMessage.builder().message("0").build());
        client.sendEvent("lock", PushMessage.builder().message("2").build()); // 当前已经lock了,后续可以发送详细锁住人信息
        room.unlock();
    }
}

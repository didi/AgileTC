package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.xiaoju.framework.util.BitBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

public abstract class IngressTask implements Runnable{
    protected static final Logger LOGGER = LoggerFactory.getLogger(IngressTask.class);

    SocketIOClient client;

    SocketIOServer socketIOServer;

    ObjectMapper jsonMapper;

    JsonNodeFactory FACTORY;

    RoomEntity room;

    ExecutorService executorEgressService;

    Lock lock;


    public IngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService) {
        this.client = client;
        this.socketIOServer = socketIOServer;
        this.room = room;
        this.executorEgressService = executorEgressService;
        this.jsonMapper = new ObjectMapper();
        this.FACTORY = JsonNodeFactory.instance;
    }

    @Override
    public void run() {

    }

    protected ClientEntity getRoomFromClient(SocketIOClient client) {
        ClientEntity clientEntity = new ClientEntity();
        String caseId = client.getHandshakeData().getSingleUrlParam("caseId");
        String recordId = client.getHandshakeData().getSingleUrlParam("recordId");
        clientEntity.setCaseIdStr(caseId);
        clientEntity.setCaseId(Long.valueOf(caseId));
        if (recordId != null) {
            clientEntity.setRecordId(Long.valueOf(recordId));
            clientEntity.setRecordIdStr(recordId);
            clientEntity.setRoomId(String.valueOf(BitBaseUtil.mergeLong(clientEntity.getRecordId(), clientEntity.getCaseId())));
        } else {
            clientEntity.setRoomId(caseId);
        }
        return clientEntity;
    }

    protected ArrayNode patchTraverse(ArrayNode patch) {
        ArrayNode patchesNew = FACTORY.arrayNode();
        try {
            for (int i = 0; i < patch.size(); i++) {
                patchesNew.addAll((ArrayNode) patch.get(i));
            }
        } catch (Exception e) {
            LOGGER.error("转换客户端发送patch失败。", e);
        }
        return patchesNew;
    }
}

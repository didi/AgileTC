package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.CompatibilityFlags;
import com.flipkart.zjsonpatch.JsonPatch;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;

import static com.flipkart.zjsonpatch.CompatibilityFlags.*;

public class RedoIngressTask extends IngressTask {
    PushMessage data;

    public RedoIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService, PushMessage data) {
        super(client, socketIOServer, room, executorEgressService);
        this.data = data;
    }
    @Override
    public void run() {
        room.lock();
        LOGGER.info(data.getMessage());
        try {
            ArrayNode patch = (ArrayNode) jsonMapper.readTree(data.getMessage());
            JsonNode roomContent = jsonMapper.readTree(room.getCaseContent());
            EnumSet<CompatibilityFlags> flags = CompatibilityFlags.defaults();
            flags.add(MISSING_VALUES_AS_NULLS);
            flags.add(ALLOW_MISSING_TARGET_OBJECT_ON_REPLACE);
            flags.add(REMOVE_NONE_EXISTING_ARRAY_ELEMENT);
            JsonNode roomContentNew = JsonPatch.apply(patch, roomContent, flags);
            room.setCaseContent(roomContentNew.toString());

            ClientEntity clientEntity = getRoomFromClient(client);
            String roomId = clientEntity.getRoomId();
            BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);
            LOGGER.info("get redo info");
            broadcastOperations.sendEvent("redo", client, PushMessage.builder().message(data.getMessage()).build());

        } catch (Exception e) {
            LOGGER.error("redo error");

        } finally {
            room.unlock();
        }

    }
}

package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;

public class RecordClearIngressTask extends IngressTask {

    public RecordClearIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService) {
        super(client, socketIOServer, room, executorEgressService);
    }

    @Override
    public void run() {
        BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(room.getRoomId());

        String caseCurrent = room.getCaseContent();

        try {
            JsonNode caseObj = jsonMapper.readTree(caseCurrent);
            JsonNode caseTarget = caseObj.deepCopy();
            traverse(caseTarget);
            ArrayNode patchNotify = (ArrayNode) JsonDiff.asJson(caseObj, caseTarget);
            executorEgressService.submit(new NotifyEgressTask("edit_notify_event", PushMessage.builder().message(patchNotify.toString()).build(), broadcastOperations));

            room.setCaseContent(caseTarget.toString());

        } catch (Exception e) {

        }
    }

    private void traverse(JsonNode caseObj) {
        Iterator<JsonNode> iterator = caseObj.iterator();

        while (iterator.hasNext()) {
            JsonNode n = iterator.next();
            if (n.size() > 0) {
                if (n.has("progress")) {
                    ((ObjectNode) n).remove("progress");
                }
                traverse(n);
            } else {
//                 System.out.println(n.toString());
            }
        }
    }
}

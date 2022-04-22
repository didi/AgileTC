package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;

import static com.flipkart.zjsonpatch.DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE;
import static com.flipkart.zjsonpatch.DiffFlags.OMIT_MOVE_OPERATION;

public class EditIngressTask extends IngressTask {
    EditMessage data;

    public EditIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService, EditMessage data) {
        super(client, socketIOServer, room, executorEgressService);
        this.data = data;
    }

    @Override
    public void run() {
        room.lock();
        LOGGER.info(data.getPatch());
        ClientEntity clientEntity = getRoomFromClient(client);
        String roomId = clientEntity.getRoomId();

        BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);

        try {
            ArrayNode patch = (ArrayNode) jsonMapper.readTree(data.getPatch());
            JsonNode roomContent = jsonMapper.readTree(room.getCaseContent());
            int serverCaseCurrentVersion = roomContent.get("base").asInt();
            int serverCaseExpectVersion = serverCaseCurrentVersion + 1;

            ArrayNode patchNew = patchTraverse(patch);
            ObjectNode basePatch = FACTORY.objectNode();
            basePatch.put("op", "replace");
            basePatch.put("path", "/base");
            basePatch.put("value", serverCaseExpectVersion);
            patchNew.add(basePatch);

            JsonNode roomContentNew;

            if (serverCaseCurrentVersion > data.getCaseVersion()) { // 服务端版本大于前端
                LOGGER.warn("version of case in memory is bigger than client. version is: " + roomContent.get("base").asInt() + ", client version: " + data.getCaseVersion());
                roomContentNew = JsonPatch.apply(patchNew, roomContent);

                ArrayNode patchAck = (ArrayNode) JsonDiff.asJson(jsonMapper.readTree(data.getCaseContent()), roomContentNew, EnumSet.of(ADD_ORIGINAL_VALUE_ON_REPLACE, OMIT_MOVE_OPERATION));
                executorEgressService.submit(new AckEgressTask("edit_ack_event", PushMessage.builder().message(patchAck.toString()).build(), client));
                executorEgressService.submit(new NotifyExcludeEgressTask("edit_notify_event", PushMessage.builder().message(patchNew.toString()).build(), client, broadcastOperations));
//                    client.sendEvent("edit_ack_event", PushMessage.builder().message(patchAck.toString()).build(), PushMessage.builder().message(patchNew.toString()).build());
//                    broadcastOperations.sendEvent("edit_notify_event", client, PushMessage.builder().message(patchNew.toString()).build());
            } else { // 服务端版本小于等于前端，中间链接断开，导致变更未传递到后端
                LOGGER.warn("version of case in memory is smaller than client. version is: " + roomContent.get("base").asInt() + ", client version: " + data.getCaseVersion());
                // todo：为避免丢失，此处应该先保存客户端的case
                String clientExceptContent = data.getCaseContent().replace("\"base\":" + data.getCaseVersion(), "\"base\":" + (data.getCaseVersion() + 1));
                roomContentNew = jsonMapper.readTree(clientExceptContent);
                ArrayNode patchNotify = (ArrayNode) JsonDiff.asJson(roomContent, jsonMapper.readTree(clientExceptContent), EnumSet.of(ADD_ORIGINAL_VALUE_ON_REPLACE, OMIT_MOVE_OPERATION));
                executorEgressService.submit(new AckEgressTask("edit_ack_event", PushMessage.builder().message("[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (data.getCaseVersion() + 1) + "}]]").build(), client));
                executorEgressService.submit(new NotifyExcludeEgressTask("edit_notify_event", PushMessage.builder().message(patchNotify.toString()).build(), client, broadcastOperations));

            }

            room.setCaseContent(roomContentNew.toString());

        } catch (Exception e) {
            LOGGER.error("json 操作失败。", e);
            executorEgressService.submit(new AckEgressTask("warning", PushMessage.builder().message("可能存在编辑冲突，请刷新重试.").build(), client));
//            client.sendEvent("warning", PushMessage.builder().message("可能存在编辑冲突，请刷新重试.").build());
        } finally {
            room.unlock();
        }
    }

}

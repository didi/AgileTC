package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonPatch;
import com.xiaoju.framework.util.JsonNodeOp;
import com.xiaoju.framework.util.PairWise;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class CaseDesignIngressTask extends IngressTask{
    CaseDesignMessage data;

    public CaseDesignIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService, CaseDesignMessage data) {
        super(client, socketIOServer, room, executorEgressService);
        this.data = data;
    }

    @Override
    public void run() {
        room.lock();
        LOGGER.info("接收到来自客户端用例设计信息:" + data.toString());
        String msgCode = data.getMethod();
        String nodeId = data.getNodeId();
        String content = data.getMessage();
        ClientEntity clientEntity = getRoomFromClient(client);
        String roomId = clientEntity.getRoomId();
        String caseContent = room.getCaseContent();
        ArrayNode patch = FACTORY.arrayNode();

        switch (msgCode) {

            case "pariwise" :
                LOGGER.info("用户输入：" + content);
                List<String> pairWiseCases = PairWise.solution(content);
                if (pairWiseCases.size() == 0) {
                    LOGGER.error("未生成用例。");
                    break;
                }

                LOGGER.info("生成case：" + pairWiseCases);
                LOGGER.info("nodeid：" + nodeId);

                patch = JsonNodeOp.generatePatch(caseContent, nodeId, pairWiseCases);

                break;
            default:
                break;
        }

        try {
            if (patch.size() == 0) {
                return;
            }
            LOGGER.info("发送的patch：" + patch.toString());
            JsonNode target = JsonPatch.apply(patch, jsonMapper.readTree(caseContent));
            room.setCaseContent(target.toString());
//            BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);
            executorEgressService.submit(new NotifyEgressTask("edit_notify_event",
                    PushMessage.builder().message(patch.toString()).build(), socketIOServer.getRoomOperations(roomId)));

        } catch (Exception e) {
            LOGGER.error("服务端合并patch异常：", e);
            room.unlock();
        }

        room.unlock();
    }
}

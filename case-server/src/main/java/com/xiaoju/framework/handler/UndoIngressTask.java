package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.CompatibilityFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;

import static com.flipkart.zjsonpatch.CompatibilityFlags.*;
import static com.flipkart.zjsonpatch.DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE;
import static com.flipkart.zjsonpatch.DiffFlags.OMIT_MOVE_OPERATION;

public class UndoIngressTask extends IngressTask {
    PushMessage data;

    public UndoIngressTask(SocketIOClient client, SocketIOServer socketIOServer, RoomEntity room, ExecutorService executorEgressService, PushMessage data) {
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
            LOGGER.info("get undo info");
            broadcastOperations.sendEvent("undo", client, PushMessage.builder().message(data.getMessage()).build());

        } catch (Exception e) {
            LOGGER.error("undo error");

        } finally {
            room.unlock();
        }
    }

    public static void main(String[] args) {
        String patch = "[{\"op\":\"replace\",\"path\":\"/base\",\"value\":162},{\"op\":\"replace\",\"path\":\"/root/children/0/children/0/children/1/children/0/data/priority\",\"value\":2}]";
        String content = "{\"root\":{\"data\":{\"created\":1669199611709,\"text\":\"查酒频次配置优化\",\"id\":\"57mb9fudv61682grgejqsccdvb\",\"expandState\":\"expand\"},\"children\":[{\"data\":{\"created\":1669199611717,\"text\":\"逻辑交互\",\"id\":\"0e97nsn07v0ifro18b0jf1c9b2\",\"expandState\":\"expand\",\"priority\":1},\"children\":[{\"data\":{\"id\":\"cqqtxfg14bc0\",\"created\":1677245556248,\"text\":\"啊啊\",\"priority\":2},\"children\":[{\"data\":{\"id\":\"cqqu3jg4oc80\",\"created\":1677246035146,\"text\":\"4\"},\"children\":[{\"data\":{\"id\":\"d07rrni373s0\",\"created\":1711871406605,\"text\":\"fff\",\"priority\":3},\"children\":[{\"data\":{\"id\":\"d0agf41ee9c0\",\"created\":1712144071528,\"text\":\"44\"},\"children\":[]}]},{\"data\":{\"id\":\"d07rxsvpnko0\",\"created\":1711871888498,\"text\":\"dd\",\"hyperlink\":\"http://image.baidu.com\"},\"children\":[{\"data\":{\"id\":\"d0agf0ovji00\",\"created\":1712144064241,\"text\":\"abc\"},\"children\":[]}]},{\"data\":{\"id\":\"d0agf78ggz5s\",\"created\":1712144078486,\"text\":\"io\",\"resource\":null,\"priority\":1},\"children\":[]}]},{\"data\":{\"id\":\"d074zy04y9k0\",\"created\":1711807170850,\"text\":\"10\"},\"children\":[{\"data\":{\"id\":\"d07rsiih7xs0\",\"created\":1711871474109,\"text\":\"dddd \"},\"children\":[{\"data\":{\"id\":\"d07rxmqbwtc0\",\"created\":1711871875112,\"text\":\"分支主题\",\"priority\":null},\"children\":[]}]}]},{\"data\":{\"id\":\"cqqtxzd581s0\",\"created\":1677245599609,\"text\":\"0\"},\"children\":[{\"data\":{\"id\":\"d07rrd5ljio0\",\"created\":1711871384082,\"text\":\"3\"},\"children\":[]},{\"data\":{\"id\":\"d07rrptmaow0\",\"created\":1711871411656,\"text\":\"ddd\"},\"children\":[]}]}]}]}]},\"template\":\"right\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":160,\"right\":1}";
        try {
            ObjectMapper jsonMapper = new ObjectMapper();

            ArrayNode patchNode = (ArrayNode) jsonMapper.readTree(patch);
            JsonNode roomContent = jsonMapper.readTree(content);
            EnumSet<CompatibilityFlags> flags = CompatibilityFlags.defaults();
            flags.add(MISSING_VALUES_AS_NULLS);
            flags.add(ALLOW_MISSING_TARGET_OBJECT_ON_REPLACE);
            JsonNode roomContentNew = JsonPatch.apply(patchNode, roomContent, flags);
            System.out.println(roomContentNew.toString());
        } catch (Exception e) {
            System.out.println("error");
            e.printStackTrace();
        }
    }
}

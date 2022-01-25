package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class JsonNodeOp {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNodeOp.class);
    private static JsonNodeFactory FACTORY = JsonNodeFactory.instance;
    private static ObjectMapper jsonMapper = new ObjectMapper();

    private static String getNodePath(JsonNode caseContent, String nodeId, Map<String, JsonNode> relatedObj) {
        JsonNode nodeObj = caseContent.get("root");
        String path = "";
        List<Integer> pathArr = new ArrayList<>();

        boolean ret = TreeUtil.getNodePath(nodeObj, nodeId, pathArr, relatedObj);
        LOGGER.info("路径栈信息：" + pathArr);
        if (ret) {
            if (relatedObj.containsKey("parentNode")) {
                path = pathArr.stream().map(f -> f.toString()).collect(Collectors.joining("/children/"));
                path = "/root/children/" + path;
            } else {
                path = "/root";
            }
        }
        LOGGER.info("计算得到的路径：" + path);
        return path;
    }
    public static ArrayNode generatePatch(String caseContent, String nodeId, List<String> caseAdd) {
        try {
            JsonNode caseObj = jsonMapper.readTree(caseContent);
            Map<String, JsonNode> relatedObj = new HashMap<>();
            String path = getNodePath(caseObj, nodeId, relatedObj);
            ArrayNode patch = FACTORY.arrayNode();
            if (path.length() == 0) {
                return patch;
            }
            for (int i = 0; i < caseAdd.size(); i ++) {
                ObjectNode node = FACTORY.objectNode();
                ObjectNode value = FACTORY.objectNode();
                node.put("op", "add");
                node.put("path", path+"/children/"+i);
                value.set("data", newNodeData(caseAdd.get(i)));
                value.set("children", FACTORY.arrayNode());
                node.set("value", value);
                patch.add(node);
            }
            if (patch.size() != 0) {
                patch.add(baseNode(caseObj.get("base").intValue()));
            }

            return patch;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("生成操作补丁异常", e);
            return null;
        }
    }


    private static JsonNode newNodeData() {
        ObjectNode data = FACTORY.objectNode();
        data.put("id", UUID.randomUUID().toString());
        data.put("created", System.currentTimeMillis());
        data.put("text", "新增节点");

        return data;
    }

    private static JsonNode newNodeData(String text) {
        ObjectNode data = FACTORY.objectNode();
        data.put("id", UUID.randomUUID().toString());
        data.put("created", System.currentTimeMillis());
        data.put("text", text);

        return data;
    }

    private static ObjectNode baseNode(int version) {
        ObjectNode base = FACTORY.objectNode();
        base.put("op", "replace");
        base.put("path", "/base");
        base.put("value", version + 1);
        return base;
    }

    public static void main(String args[]) {
        List<Integer> path = new ArrayList<>();
//        List<String> pairWiseCases = PairWise.solution("{\"1\":\"1,2\",\"2\":\"2,3\"}");
        String caseContent = "{\"root\":{\"data\":{\"created\":\"1635321458093\",\"id\":\"5a83dg8d46qifni7ep9n86t53q\",\"imageSize\":{},\"text\":\"【体脂秤】\",\"priority\":1,\"resource\":[\"执行步骤\",\"前置条件\"],\"progress\":9},\"children\":[{\"data\":{\"id\":\"23e1cedf-edc0-4a3c-8568-10f203d56626\",\"created\":1641568705345,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"created\":\"1635305726309\",\"id\":\"3ua7go3k810c6brd133msigv82\",\"imageSize\":{},\"text\":\"其他\",\"priority\":0},\"children\":[{\"data\":{\"created\":\"1635140327581\",\"id\":\"4j7qjlfjggc0a277hmmlqhvtp5\",\"imageSize\":{},\"text\":\"需求\",\"priority\":0},\"children\":[{\"data\":{\"id\":\"ece0812d-bbc5-4bc9-a8c0-936861e785e1\",\"created\":1641568690346,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"cgzjmj8lreg0\",\"created\":1641568946610,\"text\":\"分支主题\"},\"children\":[{\"data\":{\"id\":\"a3343529-5be6-48cb-97ab-f944f22bfd0d\",\"created\":1641569721918,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"1cfe2cc5-ebdf-41fc-b940-07266ed38e8c\",\"created\":1641569654862,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"cfrvszu9syw0\",\"created\":1637134668159,\"text\":\"123\",\"expandState\":\"collapse\"},\"children\":[{\"data\":{\"id\":\"6701021e-6ff3-4c5f-9888-8de1835f2965\",\"created\":1641569667678,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"df0becac-9d77-43c4-aaf8-970e164585a8\",\"created\":1641569661313,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"cgwj0sg7zsg0\",\"created\":1641262562780,\"text\":\"1234\"},\"children\":[{\"data\":{\"id\":\"aa29d934-c966-49e3-b9fa-c74b21610ea2\",\"created\":1641568656189,\"text\":\"新增节点\"},\"children\":[]}]},{\"data\":{\"id\":\"cgwj92pwkvs0\",\"created\":1641263212047,\"text\":\"123\"},\"children\":[]},{\"data\":{\"id\":\"cgzjvssarog0\",\"created\":1641569672669,\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"id\":\"cgzjwo8ha8g0\",\"created\":1641569741128,\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"id\":\"cgzjmy149i80\",\"created\":1641568978809,\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"id\":\"cfrw10u5seo0\",\"created\":1637135297243,\"text\":\"123\"},\"children\":[]},{\"data\":{\"id\":\"cgzjjchpu3uo\",\"created\":1641568696831,\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":\"1635150011992\",\"id\":\"4il0m4se3a860g69u4qtfqa043\",\"imageSize\":{},\"text\":\"孕妇模式交互\",\"priority\":0},\"children\":[{\"data\":{\"id\":\"a7fd6a0e-eba7-4241-92a8-f8ae1ee21c00\",\"created\":1641568880316,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"8bf46d69-f914-4cf9-97ed-6a317574f31c\",\"created\":1641568679613,\"text\":\"新增节点\"},\"children\":[]},{\"data\":{\"id\":\"cgsb05euuww0\",\"created\":1640833703926,\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"id\":\"cgsb195q7vk0\",\"created\":1640833790445,\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"id\":\"cgzjlmzuys80\",\"created\":1641568876424,\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":\"1635150036599\",\"id\":\"79l9dhbm8aamk8opb3kv17vecu\",\"imageSize\":{},\"text\":\"抱婴模式交互\",\"priority\":0},\"children\":[]},{\"data\":{\"created\":\"1635305863727\",\"id\":\"0mqil01rqns6v83ed4njbqnmr9\",\"imageSize\":{},\"text\":\"备注\",\"priority\":0},\"children\":[{\"data\":{\"created\":\"1635235627895\",\"id\":\"4n99e7gbgmp6v799l3cusredi3\",\"imageSize\":{},\"text\":\"身份切换和开启关闭孕妇模式都需要重新上称后，才能将体脂秤状态变更\",\"priority\":0},\"children\":[]},{\"data\":{\"created\":\"1635235848785\",\"id\":\"3l3b33qeibk01gbmoid4qvd95i\",\"imageSize\":{},\"text\":\"切换身份多入口，只测试一处即可\",\"priority\":0},\"children\":[{\"data\":{\"created\":\"1635235883099\",\"id\":\"026es7l936e8cirp2h0f73vo3g\",\"imageSize\":{},\"text\":\"首页-更多工具\",\"priority\":0},\"children\":[]},{\"data\":{\"created\":\"1635235848135\",\"id\":\"0pi11lqsuim6dm5h872o8nkulj\",\"imageSize\":{},\"text\":\"记录tab\",\"priority\":0},\"children\":[]},{\"data\":{\"created\":\"1635235871453\",\"id\":\"0ti7kf62ii0ho783k1bo997934\",\"imageSize\":{},\"text\":\"我tab-我的资料\",\"priority\":0},\"children\":[]}]},{\"data\":{\"created\":\"1635305845896\",\"id\":\"7igph10ia4o2l06c0rac6re6ek\",\"imageSize\":{},\"text\":\"数据存本地，被清理\",\"priority\":0},\"children\":[{\"data\":{\"created\":\"1635305822424\",\"id\":\"1ckejlfqnhsmea8dtc1eun3ncl\",\"imageSize\":{},\"text\":\"在手机-设置-清理应用数据\",\"priority\":0},\"children\":[]},{\"data\":{\"created\":\"1635305829923\",\"id\":\"4egqm9jcfr5kd7jspgjapom769\",\"imageSize\":{},\"text\":\"卸载重装\",\"priority\":0},\"children\":[]}]}]},{\"data\":{\"id\":\"cfkbpdojmkg0\",\"created\":1636367042936,\"text\":\"今天开始测试 \"},\"children\":[{\"data\":{\"id\":\"cfkbpikpdwg0\",\"created\":1636367053588,\"text\":\"测试过程繁琐\"},\"children\":[]},{\"data\":{\"id\":\"cfkbplomecg0\",\"created\":1636367060355,\"text\":\"测试执行\"},\"children\":[]}]}]}]},\"template\":\"right\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":88,\"right\":1}";
        String nodeId = "4j7qjlfjggc0a277hmmlqhvtp5";

//        ArrayNode ret = generatePatch(caseC, "9a59d54a-a7cb-42db-8efc-117682edc1bb", pairWiseCases);
//        String msg = "pariwise|5a83dg8d46qifni7ep9n86t53q|{\"11\":\"11,22\",\"323\":\"fs,221\"}";
//        int msgCodeIndex = msg.indexOf('|');
//        String msgCode = msg.substring(0, msgCodeIndex);
//        String msgBack = msg.substring(msgCodeIndex + 1);
//        int nodeIdIndex = msgBack.indexOf('|');
//        String nodeId = msgBack.substring(0, nodeIdIndex);
//        String content = msgBack.substring(nodeIdIndex + 1);
    }
}

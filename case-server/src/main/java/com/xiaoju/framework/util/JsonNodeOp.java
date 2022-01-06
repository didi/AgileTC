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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JsonNodeOp {


    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNodeOp.class);

    public static ArrayNode generatePatch(String caseContent, String nodeId, List<String> caseAdd) {
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNodeFactory FACTORY = JsonNodeFactory.instance;
        try {
            JsonNode caseObj = jsonMapper.readTree(caseContent);
            JsonNode nodeObj = caseObj.get("root");
            String path = "";
            List<Integer> pathArr = new ArrayList<>();
            boolean ret = TreeUtil.getNodePath(nodeObj, nodeId, pathArr);
            LOGGER.info("路径栈信息：" + pathArr);
            if (ret && pathArr.size() >= 1) {
                path = pathArr.stream().map(f->f.toString()).collect(Collectors.joining("/children/"));
                path = "/root/children/" + path;
            } else {
                path = "/root";
            }
            LOGGER.info("计算得到的路径：" + path);
            ArrayNode patch = FACTORY.arrayNode();
            for (int i = 0; i < caseAdd.size(); i ++) {
                ObjectNode node = FACTORY.objectNode();
                ObjectNode data = FACTORY.objectNode();
                ObjectNode value = FACTORY.objectNode();
                node.put("op", "add");
                node.put("path", path+"/children/"+i);
                data.put("id", UUID.randomUUID().toString());
                data.put("created", System.currentTimeMillis());
                data.put("text", caseAdd.get(i));
                value.set("data", data);
                value.set("children", FACTORY.arrayNode());
                node.set("value", value);
                patch.add(node);
            }
            if (patch.size() != 0) {
                ObjectNode node = FACTORY.objectNode();
                node.put("op", "replace");
                node.put("path", "/base");
                node.put("value", caseObj.get("base").intValue() + 1);
                patch.add(node);
            }

            return patch;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("生成操作补丁异常", e);
            return null;
        }
    }

    public static void main(String args[]) {
        List<Integer> path = new ArrayList<>();
        List<String> pairWiseCases = PairWise.solution("{\"1\":\"1,2\",\"2\":\"2,3\"}");
        String caseC = "{\"root\":{\"data\":{\"created\":1639540809311,\"text\":\"t测试.xlsx\",\"id\":\"248b4178-7243-46dc-9d94-05f35c84c758\"},\"children\":[{\"data\":{\"created\":1639540809429,\"text\":\"登录注册功能测试\",\"id\":\"eb730e2a-4378-46dc-ba63-bfcbf7ff022f\"},\"children\":[{\"data\":{\"created\":1639540809429,\"text\":\"登录模块测试\",\"id\":\"2548e672-65ee-429b-bcb1-ab7375b9d0a3\"},\"children\":[{\"data\":{\"created\":1639540809429,\"text\":\"登录成功测试\",\"id\":\"ba3e79e2-b785-48f5-ab73-108b9731fcfd\"},\"children\":[{\"data\":{\"note\":\"创建人: tester1\\n用例描述: 登录页输入正确密码，测试功能\\n备注: \\n用例是否自动化自动化: 否\\n用例关联接口: \\n用例测试类型: 功能测试\\n用例关联项目: xx登录一期\\n\",\"resource\":[\"登录\"],\"created\":1639540809430,\"text\":\"登录页登录功能测试1\",\"id\":\"35cf74a5-b351-4acc-ba40-0d3a8c0d7b85\",\"priority\":1},\"children\":[{\"data\":{\"resource\":[\"前置条件\"],\"created\":1639540809430,\"text\":\"已注册账号\",\"id\":\"629225f9-6d80-4d07-ac42-4b8018bcc44e\"},\"children\":[{\"data\":{\"resource\":[\"执行步骤\"],\"created\":1639540809430,\"text\":\"1.输入用户名\\n2.输入密码\\n3.点击登录按钮\\n\",\"id\":\"fb3e8d44-34a0-45c8-8226-6af62bcce6da\"},\"children\":[{\"data\":{\"resource\":[\"预期结果\"],\"created\":1639540809430,\"text\":\"1.登录成功\\n2.跳转到首页\",\"id\":\"6242c9b4-a454-413f-a34a-33e757597352\"},\"children\":[]}]}]}]},{\"data\":{\"note\":\"创建人: tester1\\n用例描述: 登录页输入正确密码，测试功能\\n备注: \\n用例是否自动化自动化: 否\\n用例关联接口: \\n用例测试类型: 功能测试\\n用例关联项目: xx登录一期\\n\",\"resource\":[\"登录\"],\"created\":1639540809431,\"text\":\"登录页登录功能测试2\",\"id\":\"fde2410b-eae6-485c-b79f-629e55648841\",\"priority\":1},\"children\":[{\"data\":{\"resource\":[\"前置条件\"],\"created\":1639540809431,\"text\":\"已注册账号\",\"id\":\"4f1f37a0-4b00-4768-9ac3-0d0e4c9f0561\"},\"children\":[{\"data\":{\"resource\":[\"执行步骤\"],\"created\":1639540809431,\"text\":\"1.输入用户名\\n2.输入密码\\n3.点击登录按钮\\n\",\"id\":\"b32fd6a9-aa79-4085-b19c-2eeae24b9a1a\"},\"children\":[{\"data\":{\"resource\":[\"预期结果\"],\"created\":1639540809431,\"text\":\"1.登录成功\\n2.跳转到首页\",\"id\":\"d1f821b2-5e25-4f2e-a85c-a4b1dc924116\"},\"children\":[]}]}]}]}]},{\"data\":{\"created\":1639540809431,\"text\":\"登录失败测试\",\"id\":\"1c50cd52-8144-48dc-8db4-5890356e4ffc\"},\"children\":[{\"data\":{\"note\":\"创建人: tester1\\n用例描述: 登录页输入错误密码场景，测试功能\\n备注: \\n用例是否自动化自动化: 否\\n用例关联接口: \\n用例测试类型: 功能测试\\n用例关联项目: xx登录一期\\n\",\"resource\":[\"登录\"],\"created\":1639540809433,\"text\":\"登录页登录功能测试2\",\"id\":\"1c16ce08-2f10-49e1-a7e4-dfff4b96683f\",\"priority\":1},\"children\":[{\"data\":{\"resource\":[\"前置条件\"],\"created\":1639540809433,\"text\":\"已注册账号\",\"id\":\"1323c287-b1a7-48e9-94e2-8960ac1ac05b\"},\"children\":[{\"data\":{\"resource\":[\"执行步骤\"],\"created\":1639540809433,\"text\":\"1.输入用户名\\n2.输入错误的密码\\n3.点击登录按钮\\n\",\"id\":\"45c613c5-e090-4fd1-8735-01b49e29aa99\"},\"children\":[{\"data\":{\"resource\":[\"预期结果\"],\"created\":1639540809433,\"text\":\"1.登录失败\\n2.提示密码错误\",\"id\":\"b942c61d-6a38-4dba-b9db-5950d6c6d940\"},\"children\":[]}]}]}]}]}]},{\"data\":{\"created\":1639540809433,\"text\":\"注册模块测试\",\"id\":\"e4cf5e4a-8f0b-4c00-968b-cd9c11ba315c\"},\"children\":[{\"data\":{\"created\":1639540809434,\"text\":\"注册测试\",\"id\":\"861da57d-8342-48b0-9afd-21c048cdee71\"},\"children\":[{\"data\":{\"note\":\"创建人: tester2\\n用例描述: \\n备注: 增加1个备注信息\\n用例是否自动化自动化: 是\\n用例关联接口: /api/xxx\\n用例测试类型: 可靠性测试\\n用例关联项目: xxx注册需求N期\\n\",\"resource\":[\"注册\",\"忘记密码\"],\"created\":1639540809435,\"text\":\"登录页注册功能测试\",\"id\":\"e7ecb4a1-cdfd-47d1-bacb-3ab2fe0f87a6\",\"priority\":2},\"children\":[{\"data\":{\"resource\":[\"前置条件\"],\"created\":1639540809435,\"text\":\"暂无\",\"id\":\"a8d67513-a554-48dd-9eb0-57fc3625d3a0\"},\"children\":[{\"data\":{\"resource\":[\"执行步骤\"],\"created\":1639540809435,\"text\":\"1.点击注册按钮\\n2.输入用户名\\n3.输入密码\\n4.点击确认按钮\",\"id\":\"3b8ff894-acaa-4571-96f8-9dcfdddda74d\"},\"children\":[{\"data\":{\"resource\":[\"预期结果\"],\"created\":1639540809435,\"text\":\"1.注册成功\\n2.跳转到首页\",\"id\":\"aa613e5c-edee-4e16-8d96-24d58263c695\"},\"children\":[]}]}]}]}]}]}]},{\"data\":{\"created\":1639540809435,\"text\":\"登录注册功能测试1\",\"id\":\"9a59d54a-a7cb-42db-8efc-117682edc1bb\"},\"children\":[{\"data\":{\"created\":1639540809435,\"text\":\"登录模块测试1\",\"id\":\"13999fd2-a04b-4f4e-a4fb-a055035b3f2d\",\"priority\":3},\"children\":[{\"data\":{\"created\":1639540809435,\"text\":\"登录成功测试1\",\"id\":\"6e5a9e2b-4c79-48eb-83a8-4dd369f318c1\"},\"children\":[{\"data\":{\"note\":\"创建人: tester1\\n用例描述: 登录页输入正确密码，测试功能\\n备注: \\n用例是否自动化自动化: 否\\n用例关联接口: \\n用例测试类型: 功能测试\\n用例关联项目: xx登录一期\\n\",\"resource\":[\"登录\"],\"created\":1639540809436,\"text\":\"登录页登录功能测试2\",\"id\":\"57cd8d88-c172-46e1-adb6-90f77a2a7586\"},\"children\":[{\"data\":{\"resource\":[\"前置条件\"],\"created\":1639540809436,\"text\":\"已注册账号\",\"id\":\"0a769566-a3ca-43af-b552-79d091a6b2e8\"},\"children\":[{\"data\":{\"resource\":[\"执行步骤\"],\"created\":1639540809436,\"text\":\"1.输入用户名\\n2.输入密码\\n3.点击登录按钮\\n\",\"id\":\"8076682a-196a-49dc-8e3b-a39f85173488\"},\"children\":[{\"data\":{\"resource\":[\"预期结果\"],\"created\":1639540809436,\"text\":\"1.登录成功\\n2.跳转到首页\",\"id\":\"7159756e-8f63-4eb5-b496-0c5fd337e0fa\"},\"children\":[]}]}]}]},{\"data\":{\"id\":\"cgrs0jp80s80\",\"created\":1640780133939,\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"id\":\"cgrs1bvtua80\",\"created\":1640780195289,\"text\":\"分支主题\"},\"children\":[]}]}]},{\"data\":{\"id\":\"cgmqk5c7buw0\",\"created\":1640268227957,\"text\":\"123\"},\"children\":[]}]}]},\"template\":\"right\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":20,\"right\":1}";
        ArrayNode ret = generatePatch(caseC, "9a59d54a-a7cb-42db-8efc-117682edc1bb", pairWiseCases);
        System.out.println(ret.toString());
        System.out.println(path);
//        String msg = "pariwise|5a83dg8d46qifni7ep9n86t53q|{\"11\":\"11,22\",\"323\":\"fs,221\"}";
//        int msgCodeIndex = msg.indexOf('|');
//        String msgCode = msg.substring(0, msgCodeIndex);
//        String msgBack = msg.substring(msgCodeIndex + 1);
//        int nodeIdIndex = msgBack.indexOf('|');
//        String nodeId = msgBack.substring(0, nodeIdIndex);
//        String content = msgBack.substring(nodeIdIndex + 1);
    }
}

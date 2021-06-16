package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum;
import com.xiaoju.framework.entity.dto.ApplyPatchResultDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.flipkart.zjsonpatch.DiffFlags.*;
import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_EXPAND_STATE_CONFLICT;
import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_REPLACE_ORDER_CONFLICT;

public class MinderJsonPatchUtil {
    // TODO: 目前混用了 fastjson 和 jackson ，后面需要优化下，把 fastjson 依赖干掉


    /**
     * 获取两个 Json 之间的差异，以 json patch 格式返回
     * @param baseContent 改动前 json
     * @param targetContent 改动后 json
     * @return json patch 格式的 patch json
     * @throws IOException json 解析错误时，抛出此异常
     */
    public static String getContentPatch(String baseContent, String targetContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String convertedBaseContent = convertChildrenArrayToObject(baseContent);
        String convertedTargetContent = convertChildrenArrayToObject(targetContent);

        JsonNode base = mapper.readTree(convertedBaseContent);
        JsonNode result = mapper.readTree(convertedTargetContent);

        // OMIT_COPY_OPERATION: 每个节点的 id 都是不一样的，界面上的 copy 到 json-patch 应该是 add ，不应该出现 copy 操作。
        // ADD_ORIGINAL_VALUE_ON_REPLACE: replace 中加一个 fromValue 表达原来的值
        // OMIT_MOVE_OPERATION: 所有 move 操作，都还是维持原来 add + remove 的状态，避免一些类似 priority 属性值的一增一减被认为是 move 。
        // 去掉了默认自带的 OMIT_VALUE_ON_REMOVE ，这样所有 remove 会在 value 字段中带上原始值
        JsonNode originPatch = JsonDiff.asJson(base, result,
                EnumSet.of(OMIT_COPY_OPERATION, ADD_ORIGINAL_VALUE_ON_REPLACE, OMIT_MOVE_OPERATION));

        // 借助去掉 order 的内容，正确生成 move 操作
        JsonNode baseWithoutOrder = mapper.readTree(convertChildrenArrayToObject(baseContent, false));
        JsonNode targetWithoutOrder = mapper.readTree(convertChildrenArrayToObject(targetContent, false));

        List<String> allFromPath = new ArrayList<>();
        List<String> allToPath = new ArrayList<>();
        List<JsonNode> allMoveOprations = new ArrayList<>();

        // 需要生成 move 操作，去掉原有 flags 里面的忽略 move 标记
        JsonNode noOrderPatch = JsonDiff.asJson(baseWithoutOrder, targetWithoutOrder,
                EnumSet.of(OMIT_COPY_OPERATION, ADD_ORIGINAL_VALUE_ON_REPLACE));
        for (JsonNode oneNoOrderPatch: noOrderPatch) {
            if ("move".equals(oneNoOrderPatch.get("op").asText())) {
                allFromPath.add(oneNoOrderPatch.get("from").asText());
                allToPath.add(oneNoOrderPatch.get("path").asText());
                allMoveOprations.add(oneNoOrderPatch);
            }
        }

        ArrayNode finalPatch = mapper.createArrayNode();
        // 先把所有 move 加进这个最终的 patch 中
        for (JsonNode movePatch : allMoveOprations) {
            finalPatch.add(movePatch);
        }

        for (JsonNode onePatch : originPatch) {
            // 和 move 匹配的 add 中，根节点 order 字段需要变为 replace 存下来，避免丢失顺序
            if ("add".equals(onePatch.get("op").asText()) && allToPath.contains(onePatch.get("path").asText())) {
                // 获取 add 中 value 第一层的 order 值。此时 value 实际是移动的整体 object ，order 就在第一层
                int newOrder = onePatch.get("value").get("order").asInt();
                ObjectNode replaceOrderPatch = mapper.createObjectNode();
                replaceOrderPatch.put("op", "replace");
                replaceOrderPatch.put("path", onePatch.get("path").asText() + "/order");
                replaceOrderPatch.put("value", newOrder);
                // 这种情况下就不用管 replace 的原来值是什么了，所以不设定 fromValue
                finalPatch.add(replaceOrderPatch);

                // 这个 add 的作用已经被 move + replace 达成了，所以不需要记录这个 add
                continue;
            }

            // move 的源节点删除操作，需要忽略，因为 move 已经起到相应的作用了
            if ("remove".equals(onePatch.get("op").asText()) && allFromPath.contains(onePatch.get("path").asText())) {
                continue;
            }

            // 如果 order 没变，那不去除 order 的 patch 有可能也有 move 。这个时候这个 move 需要去掉，避免重复
            if ("move".equals(onePatch.get("op").asText()) && allMoveOprations.contains(onePatch)) {
                continue;
            }

            // 其他不需要调整的，直接加进去就可以了
            finalPatch.add(onePatch);
        }

        // 整体的 replace 和 remove 加上 test
        finalPatch = addTestToAllReplaceAndRemove(finalPatch);

        return mapper.writeValueAsString(finalPatch);
    }

    /**
     * 给所有 replace 或 remove 的 patch ，能校验原始值的，都加上 test
     * @param allPatch ArrayNode 形式的所有 patch 内容
     * @return 添加完 test 后的所有 patch 内容
     */
    private static ArrayNode addTestToAllReplaceAndRemove(ArrayNode allPatch) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (JsonNode onePatch : allPatch) {
            // 实际应用 patch 时，不会管 replace 本身的 fromValue 字段。得手动前面加一个 test 的校验应用前的原内容是否一致，并在外面再用一个 array 包起来。
            // 即 [.., {op: replace, fromValue: .., path: .., value: ..}] 改为 [.., [{op: test, path: .., value: <fromValue>}, {op: replace, path: .., value: <value>}]]
            // 如果没有 fromValue 字段，那无法校验，直接按原来样子记录即可
            if ("replace".equals(onePatch.get("op").asText()) && onePatch.get("fromValue") != null) {
                ArrayNode testAndReplaceArray = mapper.createArrayNode();
                ObjectNode testPatch = mapper.createObjectNode();
                testPatch.put("op", "test");
                testPatch.put("path", onePatch.get("path").asText());
                testPatch.set("value", onePatch.get("fromValue"));

                testAndReplaceArray.add(testPatch);
                testAndReplaceArray.add(onePatch);

                result.add(testAndReplaceArray);
                continue;
            }

            // remove 同理，有 value 的前面都加一个 test
            // 特别注意：在测试任务中删除时，很容易因为 order 不一致导致 test 不通过，无法删除。因此删除前校验，应该只校验删除前的本节点 data 及 childrenObject 内容
            if ("remove".equals(onePatch.get("op").asText()) && onePatch.get("value") != null) {
                ArrayNode testAndRemoveArray = mapper.createArrayNode();
                if (isNodePath(onePatch.get("path").asText())) {
                    // 移除的是节点，只需要校验本级别节点的 data 及 childrenObject 全部内容，不要校验 order
                    JsonNode originValue = onePatch.get("value");

                    ObjectNode testPatchForData = mapper.createObjectNode();
                    testPatchForData.put("op", "test");
                    testPatchForData.put("path", onePatch.get("path").asText() + "/data");
                    testPatchForData.set("value", originValue.get("data"));

                    ObjectNode testPatchForChildrenObject = mapper.createObjectNode();
                    testPatchForChildrenObject.put("op", "test");
                    testPatchForChildrenObject.put("path", onePatch.get("path").asText() + "/childrenObject");
                    testPatchForChildrenObject.set("value", originValue.get("childrenObject"));

                    testAndRemoveArray.add(testPatchForData);
                    testAndRemoveArray.add(testPatchForChildrenObject);
                } else {
                    // 移除的不是节点，正常验证全部内容即可
                    ObjectNode testPatch = mapper.createObjectNode();

                    testPatch.put("op", "test");
                    testPatch.put("path", onePatch.get("path").asText());
                    testPatch.set("value", onePatch.get("value"));

                    testAndRemoveArray.add(testPatch);
                }
                testAndRemoveArray.add(onePatch);
                result.add(testAndRemoveArray);
                continue;
            }

            result.add(onePatch);
        }

        return result;
    }

    /**
     * 详见 batchApplyPatch(String patch, String baseContent, EnumSet<ApplyPatchFlagEnum> flags) 方法。此方法使用的默认 EnumSet 为空。
     * @param patch patch json
     * @param baseContent 需要应用到的 json
     * @return ApplyPatchResultDto 对象，包含应用后的 json 、应用成功的 patch 和跳过的 patch
     * @throws IOException json 解析错误时，抛出此异常
     */
    public static ApplyPatchResultDto batchApplyPatch(String patch, String baseContent) throws IOException {
        return batchApplyPatch(patch, baseContent, EnumSet.noneOf(ApplyPatchFlagEnum.class));
    }

    /**
     * 逐个应用 patch 到目标 json 中，并自动跳过无法应用的 patch 。
     * @param patch patch json
     * @param baseContent 需要应用到的 json
     * @param flags EnumSet，每个元素为 ApplyPatchFlagEnum 枚举值。用于指代应用 patch 过程中一些特殊操作
     * @return ApplyPatchResultDto 对象，包含应用后的 json 、应用成功的 patch 和跳过的 patch
     * @throws IOException json 解析错误时，抛出此异常
     */
    public static ApplyPatchResultDto batchApplyPatch(String patch, String baseContent, EnumSet<ApplyPatchFlagEnum> flags) throws IOException {
        baseContent = convertChildrenArrayToObject(baseContent);

        ApplyPatchResultDto applyPatchResultDto = new ApplyPatchResultDto();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode patchJson = mapper.readTree(patch);
        JsonNode afterPatchJson = mapper.readTree(baseContent);
        List<String> conflictPatch = new ArrayList<>();
        List<String> applyPatch = new ArrayList<>();

        for (JsonNode onePatchOperation : patchJson) {
            try {
                if (onePatchOperation.isArray()) {
                    afterPatchJson = JsonPatch.apply(onePatchOperation, afterPatchJson);
                } else { // 外面包一个 array
                    afterPatchJson = JsonPatch.apply(mapper.createArrayNode().add(onePatchOperation), afterPatchJson);
                }
                applyPatch.add(mapper.writeValueAsString(onePatchOperation));
            } catch (JsonPatchApplicationException e) {
                // 检查是否是对 order 的操作。如果是，那就忽略这个冲突
                if (flags.contains(IGNORE_REPLACE_ORDER_CONFLICT) &&
                        onePatchOperation.isArray() &&
                        onePatchOperation.get(0).get("path").asText().endsWith("/order")) {
                    continue;
                }
                // 忽略展开/不展开节点类的改动冲突
                if (flags.contains(IGNORE_EXPAND_STATE_CONFLICT) &&
                        onePatchOperation.isArray() &&
                        onePatchOperation.get(0).get("path").asText().endsWith("/expandState")
                ){
                    continue;
                }
                conflictPatch.add(mapper.writeValueAsString(onePatchOperation));
            }
        }

        String afterPatch = mapper.writeValueAsString(afterPatchJson);
        afterPatch = convertChildrenObjectToArray(afterPatch);

        applyPatchResultDto.setJsonAfterPatch(afterPatch);
        applyPatchResultDto.setConflictPatch(conflictPatch);
        applyPatchResultDto.setApplyPatch(applyPatch);

        return applyPatchResultDto;
    }

    /**
     * 清空用例 json 中包含的所有 progress 属性，即清空里面包含的测试结果
     * @param
     * @return
     */
    public static String cleanAllProgress(String caseContent) {
        JSONObject caseContentJson = JSON.parseObject(caseContent);
        JSONObject rootData = caseContentJson.getJSONObject("root");

        removeNodeSpecificField(rootData, "progress");

        // 把旧数据直接删掉，换成新数据
        caseContentJson.remove("root");
        caseContentJson.put("root", rootData);

        return JSON.toJSONString(caseContentJson);
    }

    /**
     * 清空用例 json 中包含的所有 background 属性，避免影响后续历史记录预览结果
     * @param
     * @return
     */
    public static String cleanAllBackground(String caseContent) {
        JSONObject caseContentJson = JSON.parseObject(caseContent);
        JSONObject rootData = caseContentJson.getJSONObject("root");

        removeNodeSpecificField(rootData, "background");

        // 把旧数据直接删掉，换成新数据
        caseContentJson.remove("root");
        caseContentJson.put("root", rootData);

        return JSON.toJSONString(caseContentJson);
    }

    /**
     * 把 children 从 array 改为 object (array中每个元素外面多加一个 key ，key 的值为元素中的 data.id )，解决 json-pointer 针对数组用下标定位，会不准确问题
     * 示例：
     * 转换前：   {"root": {"data": {"id": "nodeA"}, "children": [{"data": {"id": "nodeAa"}, "children": []}, {"data": {"id": "nodeAb"}, "children": []}]}}
     * 转换后:    {"root": {"data": {"id": "nodeA"}, "childrenObject": {"nodeAa": {"data": {"id": "nodeAa"}, "childrenObject": {}, "order": 0}}, {"nodeAb": {"data": {"id": "nodeAb"}, "childrenObject": {}, "order": 1}}}}
     * @param caseContent 完整用例 json ，需包含 root 节点数据
     * @return 转换后 children 都不是 array 的新完整用例 json
     */
    public static String convertChildrenArrayToObject(String caseContent) {
        return convertChildrenArrayToObject(caseContent, true);
    }

    /**
     * 把 children 重新从 object 改为 array ，变回原来脑图的格式，用于实际存储到数据库
     * 示例：
     * 转换前：   {"root": {"data": {"id": "nodeA"}, "childrenObject": {"nodeAa": {"data": {"id": "nodeAa"}, "childrenObject": {}, "order": 0}}, {"nodeAb": {"data": {"id": "nodeAb"}, "childrenObject": {}, "order": 1}}}}
     * 转换后:    {"root": {"data": {"id": "nodeA"}, "children": [{"data": {"id": "nodeAa"}, "children": []}, {"data": {"id": "nodeAb"}, "children": []}]}}
     * @param convertedCaseContent 转换过的完整用例 json
     * @return 转换会原来脑图格式的完整 json
     */
    public static String convertChildrenObjectToArray(String convertedCaseContent) {
        return convertChildrenObjectToArray(convertedCaseContent, true);
    }

    /**
     * 根据 jsonPatch 内容，在脑图中标记变更。以节点为单位，增加的加绿色背景，删除的加红色背景，修改的加蓝色背景。
     * 特别注意，移动节点（move）因为实际节点 id 未有变化，所以也会被标记为修改
     *
     * @param minderContent
     * @param jsonPatch
     * @return
     */
    public static String markJsonPatchOnMinderContent(String jsonPatch, String minderContent) throws IOException, IllegalArgumentException {
        String green = "#67c23a";
        String blue = "#409eff";
        String red = "#f56c6c";

        ObjectMapper objectMapper = new ObjectMapper();
        // 因为 jsonPatch 是针对已经把 children 数组变为对象的 json 格式，所以要先转换下
        ObjectNode convertedMinderContentJson = objectMapper.readTree(convertChildrenArrayToObject(minderContent)).deepCopy();

        ArrayNode jsonPatchArray = (ArrayNode) objectMapper.readTree(jsonPatch);

        for (JsonNode onePatch : jsonPatchArray) {
            JsonNode operation;
            if (onePatch.isArray() && onePatch.size() <= 3 && onePatch.size() >= 2) {
                // 只可能是 replace 或 remove 的。前面多加了 test ，会是一个带有2个或3个元素的 array 。且最后一个才是 replace 或 remove
                operation = onePatch.get(onePatch.size()-1);
                if (!("replace".equals(operation.get("op").asText()) || "remove".equals(operation.get("op").asText()))) {
                    throw new IllegalArgumentException(String.format("此单个 patch 格式不正常，" +
                                    "正常格式在多元素 array 的最后一个，应该是 replace 或 remove 操作" +
                                    "不符合的 patch 内容: %s",
                            objectMapper.writeValueAsString(onePatch)));
                }
            } else if (onePatch.isObject()) {
                operation = onePatch;
            } else {
                // 目前不会生成不符合这两种格式的 patch ，抛异常
                throw new IllegalArgumentException(String.format("此单个 patch 格式不正常，正常格式应该是2到3个元素的array或单个object" +
                                "请确认 patch 内容是通过此工具类提供的获取 patch 方法生成。不符合的 patch 内容: %s",
                        objectMapper.writeValueAsString(onePatch)));
            }

            // 先判定是否为整个节点的内容变更
            if (isNodePath(operation.get("path").asText())) {
                // 节点级别，只支持 add 、 remove 、move 。因为 replace 只改值不改key，不可能在节点级别产生 replace 操作
                switch (operation.get("op").asText()) {
                    case "add":
                        addAddNodeMark(convertedMinderContentJson, operation, green);
                        break;
                    case "move":
                        addMoveNodeMark(convertedMinderContentJson, operation, blue);
                        break;
                    case "remove":
                        addRemoveNodeMark(convertedMinderContentJson, operation, red);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("此单个 patch 格式不正常，" +
                                        "正常的节点级别 patch ，op 应该是 add、move、remove 其中一个" +
                                        "不符合的 patch 内容: %s",
                                objectMapper.writeValueAsString(operation)));
                }
            } else {
                // 非节点级别变更，都将它标记为 修改内容 即可。不应该出现 move 节点属性的动作
                switch (operation.get("op").asText()) {
                    case "add":
                        addAddAttrMark(convertedMinderContentJson, operation, blue);
                        break;
                    case "replace":
                        addReplaceAttrMark(convertedMinderContentJson, operation, blue);
                        break;
                    case "remove":
                        addRemoveAttrMark(convertedMinderContentJson, operation, blue);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("此单个 patch 格式不正常，" +
                                        "正常的非节点级别 patch ，op 应该是 add、replace、remove 四个其中一个" +
                                        "不符合的 patch 内容: %s",
                                objectMapper.writeValueAsString(operation)));
                }
            }
        }

        return convertChildrenObjectToArray(objectMapper.writeValueAsString(convertedMinderContentJson));
    }

    private static void addReplaceAttrMark(JsonNode convertedMinderContentJson, JsonNode replacePatch, String backgroundValue) {
        // replace 的意味着节点 id 没变，只会影响单个节点，直接给这个节点标记即可
        // 因为改动内容是单个属性，所以直接给这个 patch 上一级的节点，把 background 属性改为蓝色即可
        String replacePathText = replacePatch.get("path").asText();
        // 改动的属性有可能是在 data 字段中的，也可能是手动添加的 order
        String replaceNodePathText;
        if (replacePathText.endsWith("order")) {
            replaceNodePathText = replacePathText.substring(0, replacePathText.lastIndexOf("/order"));
        } else {
            replaceNodePathText = replacePathText.substring(0, replacePathText.lastIndexOf("/data"));
        }

        ObjectNode replaceNode = (ObjectNode) convertedMinderContentJson.at(replaceNodePathText);
        ((ObjectNode) replaceNode.get("data")).put("background", backgroundValue);
    }

    private static void addRemoveAttrMark(JsonNode convertedMinderContentJson, JsonNode removePatch, String backgroundValue) {
        String removePath = removePatch.get("path").asText();

        // remove 属性，直接给这个节点做标记即可
        String removeAttrNodePath = removePath.substring(0, removePath.lastIndexOf("/data"));
        JsonNode removeAttrNode = convertedMinderContentJson.at(removeAttrNodePath);
        ((ObjectNode) removeAttrNode.get("data")).put("background", backgroundValue);
    }

    private static void addRemoveNodeMark(JsonNode convertedMinderContentJson, JsonNode removePatch, String backgroundValue) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String removePath = removePatch.get("path").asText();

        // remove 节点，有可能存在两种场景：应用成功、应用失败。成功的话，节点会不存在。需要根据现在新版内容判定是成功还是失败。
        JsonNode removeNode = convertedMinderContentJson.at(removePath);
        if (removeNode.isMissingNode()) {
            // 属于应用成功的，需要从 value 里把删掉的内容重新加回来，并给每个子节点都标记红色
            removeNode = removePatch.get("value");
            if (removeNode == null) {
                throw new IllegalArgumentException(String.format("此单个 patch 格式不正常，" +
                                "正常格式 remove 应该带有 value 字段，表示被删内容原始值" +
                                "不符合的 patch 内容: %s",
                        objectMapper.writeValueAsString(removePatch)));
            }
            String removePathText = removePatch.get("path").asText();
            String removeNodeParentNodePath = removePathText.substring(0, removePathText.lastIndexOf('/'));
            ObjectNode removeNodeParentObject = (ObjectNode) convertedMinderContentJson.at(removeNodeParentNodePath);
            removeNodeParentObject.set(removeNode.get("data").get("id").asText(), removeNode);

            addBackgroundOnAllNodes((ObjectNode) removeNode, backgroundValue);
        } else {
            // 属于应用失败，存在冲突的 patch 。直接标记颜色即可
            addBackgroundOnAllNodes((ObjectNode) removeNode, backgroundValue);
        }
    }

    private static void addAddAttrMark(JsonNode convertedMinderContentJson, JsonNode addPatch, String backgroundValue) {
        // add 有可能是新增子节点，也可能是新增属性。
        String addPatchPath = addPatch.get("path").asText();

        // 增加属性的
        String addPatchNodePath = addPatchPath.substring(0, addPatchPath.lastIndexOf("/data"));
        JsonNode addAttrNode = convertedMinderContentJson.at(addPatchNodePath);
        ((ObjectNode) addAttrNode.get("data")).put("background", backgroundValue);
    }

    private static void addAddNodeMark(JsonNode convertedMinderContentJson, JsonNode addPatch, String backgroundValue) {
        // 递归给本节点及所有子节点加标记
        JsonNode addNode = convertedMinderContentJson.at(addPatch.get("path").asText());
        addBackgroundOnAllNodes((ObjectNode) addNode, backgroundValue);
    }


    private static void addMoveNodeMark(JsonNode convertedMinderContentJson, JsonNode movePatch, String backgroundValue) {
        // 不管是否冲突，备份里都会有移动后的数据。找到 move 后的 object ，直接递归加标记即可
        JsonNode moveNode = convertedMinderContentJson.at(movePatch.get("path").asText());
        addBackgroundOnAllNodes((ObjectNode) moveNode, backgroundValue);
    }

    private static boolean isNodePath(String jsonPatchPath) {
        String[] keys = jsonPatchPath.split("/");
        return "childrenObject".equals(keys[keys.length - 2]);
    }


    // 递归给此节点及下面所有子节点都加上指定的 background 属性值
    private static void addBackgroundOnAllNodes(ObjectNode rootNode, String backgroundValue) {
        // 先给当前节点的 data ，加 background
        ((ObjectNode) rootNode.get("data")).put("background", backgroundValue);

        // 再给当前节点的 childrenObject ，进行递归
        for (JsonNode childObject : rootNode.get("childrenObject")) {
            addBackgroundOnAllNodes((ObjectNode) childObject, backgroundValue);
        }
    }


    private static String convertChildrenArrayToObject(String caseContent, Boolean withOrder) {
        JSONObject caseContentJson = JSON.parseObject(caseContent);
        JSONObject rootData = caseContentJson.getJSONObject("root");

        rootData.put("childrenObject", convertArrayToObject(rootData.getJSONArray("children"), withOrder));

        // 把旧数据直接删掉，换成新数据
        rootData.remove("children");

        return JSON.toJSONString(caseContentJson);
    }

    private static String convertChildrenObjectToArray(String convertedCaseContent, Boolean withOrder) {
        JSONObject caseContentJson = JSON.parseObject(convertedCaseContent, Feature.OrderedField);
        JSONObject rootData = caseContentJson.getJSONObject("root");

        rootData.put("children", convertObjectToArray(rootData.getJSONObject("childrenObject"), withOrder));

        // 把旧数据直接删掉，换成新数据
        rootData.remove("childrenObject");

        return JSON.toJSONString(caseContentJson);
    }

    // 递归把每个 object 改回 array ，去掉 object 中第一层的 key
    private static JSONArray convertObjectToArray(JSONObject childrenObject, Boolean withOrder) {
        JSONArray childrenArray = new JSONArray();
        List<String> keyMoved = new ArrayList<>();

        // object 中每个子元素，重新放回到 array 中
        for (int i=0; i<childrenObject.keySet().size(); i++) {
            for (String key : childrenObject.keySet()) {

                JSONObject child = childrenObject.getJSONObject(key);
                if (withOrder) {
                    // 需要根据 order 判定原来的顺序，按顺序加进去，避免顺序错误
                    if (Integer.valueOf(i).equals(child.getInteger("order"))) {
                        childrenArray.add(child);
                        keyMoved.add(key);
                    } else {
                        continue;
                    }
                } else {
                    // 不用管 order ，直接一个一个 key 加进去就是了
                    childrenArray.add(child);
                    keyMoved.add(key);
                }

                // 对添加的 child 进行递归，把它的 childrenObject 再变回 array
                JSONObject childrenObjectInChild = child.getJSONObject("childrenObject");
                child.put("children", convertObjectToArray(childrenObjectInChild, withOrder));

                if (withOrder) {
                    // 去掉排序用的临时字段
                    child.remove("order");
                }
                child.remove("childrenObject");
            }
        }

        // 有可能 order 值很大，上面根据 size 获取 order 值获取不到。所以最后要把剩余的 childrenObject 元素继续放到 array 里面
        for (String key : childrenObject.keySet()) {
            if (!keyMoved.contains(key)) {
                JSONObject child = childrenObject.getJSONObject(key);
                childrenArray.add(child);

                // 对添加的 child 进行递归，把它的 childrenObject 再变回 array
                JSONObject childrenObjectInChild = child.getJSONObject("childrenObject");
                child.put("children", convertObjectToArray(childrenObjectInChild, withOrder));

                if (withOrder) {
                    // 去掉排序用的临时字段
                    child.remove("order");
                }
                child.remove("childrenObject");
            }
        }

        return childrenArray;
    }

    // 递归把 array 改为 object ，key 为原来子元素的 id
    private static JSONObject convertArrayToObject(JSONArray childrenArray, Boolean withOrder) {

        // 把 children 这个 array 换成 Object
        JSONObject childrenObject = new JSONObject();

        // children 中每个子元素都变为 object
        for (int i=0; i<childrenArray.size(); i++) {
            JSONObject child = childrenArray.getJSONObject(i);
            String childId = child.getJSONObject("data").getString("id");

            if (withOrder) {
                // 加一个 order 字段，用于转回 array 时保证内部顺序一致。
                child.put("order", i);
            }
            childrenObject.put(childId, child);

            // 对 child 进行递归，把它的 children 再变成 object
            JSONArray childrenArrayInChild = child.getJSONArray("children");
            child.put("childrenObject", convertArrayToObject(childrenArrayInChild, withOrder));

            // 删掉已经不需要的 children 字段
            child.remove("children");
        }

        return childrenObject;
    }

    // 删除节点下面的自带属性，即任意 data 字段下的子属性
    private static void removeNodeSpecificField(JSONObject rootData, String fieldName) {
        rootData.getJSONObject("data").remove(fieldName);

        // 继续递归子节点
        JSONArray children = rootData.getJSONArray("children");
        for (int i=0; i<children.size(); i++) {
            removeNodeSpecificField(children.getJSONObject(i), fieldName);
        }
    }

}

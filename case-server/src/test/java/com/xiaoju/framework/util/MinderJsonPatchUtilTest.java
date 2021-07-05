package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.xiaoju.framework.entity.dto.ApplyPatchResultDto;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_EXPAND_STATE_CONFLICT;
import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_REPLACE_ORDER_CONFLICT;

public class MinderJsonPatchUtilTest {


    private String jsonNoNode;
    private String jsonAddNodeA;
    private String jsonAddNodeB;
    private String jsonAddNodeCBaseNodeB;
    private String jsonAddNodeDBaseNodeA;
    private String jsonAddABCD;
    private String jsonAddPriorityOnABaseAD;

    private String jsonChangeNodeAToAA;
    private String jsonChangeNodeAToAAA;
    private String jsonMoveBBaseAOnABCD;
    private String jsonMoveBBaseABeforeDOnABCD;

    private String jsonNodeACollapse;
    private String jsonNodeAExpand;

    private ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void initResources() throws Exception {
        jsonNoNode = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonNoNode.json")), "utf-8");
        jsonAddNodeA = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddNodeA.json")), "utf-8");
        jsonAddNodeB = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddNodeB.json")), "utf-8");;
        jsonAddNodeCBaseNodeB = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddNodeCBaseNodeB.json")), "utf-8");
        jsonAddNodeDBaseNodeA = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddNodeDBaseNodeA.json")), "utf-8");
        jsonAddABCD = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddABCD.json")), "utf-8");
        jsonAddPriorityOnABaseAD = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonAddPriorityOnABaseAD.json")), "utf-8");

        jsonChangeNodeAToAA = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonChangeNodeAToAA.json")), "utf-8");
        jsonChangeNodeAToAAA = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonChangeNodeAToAAA.json")), "utf-8");

        jsonMoveBBaseAOnABCD = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonMoveBBaseAOnABCD.json")), "utf-8");
        jsonMoveBBaseABeforeDOnABCD = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonMoveBBaseABeforeDOnABCD.json")), "utf-8");

        jsonNodeACollapse = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonNodeACollapse.json")), "utf-8");
        jsonNodeAExpand = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonNodeAExpand.json")), "utf-8");


    }

    // 测试增量合并相关功能
    @Test
    public void testGetDiffAndPatchBetweenTwoVersion() throws IOException {
        String patch = MinderJsonPatchUtil.getContentPatch(jsonNoNode, jsonAddNodeA);

        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(patch, jsonNoNode);
        Assert.assertEquals(JSONObject.parseObject(jsonAddNodeA), JSONObject.parseObject(applyPatchResultDto.getJsonAfterPatch()));
    }

    @Test
    public void testConflict() throws JsonProcessingException, IOException {
        // 最原始的 json 是 jsonAddNodeB
        String baseJson = jsonAddNodeB;

        // 首先，有用户基于 b 添加子节点 c
        String addNodeC = MinderJsonPatchUtil.getContentPatch(baseJson, jsonAddNodeCBaseNodeB);
        System.out.println("第一个 patch 内容: " + addNodeC);

        // 同时，另一个用户把 b 删除了
        String deleteB = MinderJsonPatchUtil.getContentPatch(baseJson, jsonNoNode);
        System.out.println("第二个 patch 内容: " + deleteB);

        // 删除的先保存
        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(deleteB, jsonAddNodeB);
        String firstSave = applyPatchResultDto.getJsonAfterPatch();

        // 添加的再保存，会因为冲突被跳过
        applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(addNodeC, firstSave);
        Assert.assertEquals(JSONObject.parseObject(firstSave), JSONObject.parseObject(applyPatchResultDto.getJsonAfterPatch()));
        Assert.assertNotEquals(0, applyPatchResultDto.getConflictPatch().size());
        System.out.println("产生冲突的 patch 操作" + applyPatchResultDto.getConflictPatch());
    }

    @Test // 测试在获取 patch 和应用 patch 时，整个 json 的 children 从 array 改为用 object ，是否可解决下标不准确问题
    public void testApplyPatchOnArray() throws IOException {
        String baseJson = jsonNoNode;

        // 用户a开始编辑, 添加了 A
        String addA = MinderJsonPatchUtil.getContentPatch(baseJson, jsonAddNodeA);
        String step1 = MinderJsonPatchUtil.batchApplyPatch(addA, baseJson).getJsonAfterPatch();

        // 用户b开始编辑，添加了 B
        String addB = MinderJsonPatchUtil.getContentPatch(baseJson, jsonAddNodeB);
        String step2 = MinderJsonPatchUtil.batchApplyPatch(addB, step1).getJsonAfterPatch();

        // 用户 a 继续基于 A 添加 D
        String addDbaseA = MinderJsonPatchUtil.getContentPatch(jsonAddNodeA, jsonAddNodeDBaseNodeA);
        String step3 = MinderJsonPatchUtil.batchApplyPatch(addDbaseA, step2).getJsonAfterPatch();

        // 用户 b 继续基于 B 添加 C
        String addCbaseB = MinderJsonPatchUtil.getContentPatch(jsonAddNodeB, jsonAddNodeCBaseNodeB);
        String step4 = MinderJsonPatchUtil.batchApplyPatch(addCbaseB, step3).getJsonAfterPatch();

        // 确认最后的 C 是加到了 B 而非 A 上。直接用原版 json 应用增量，会因为下标为0加到了 A 后面。
        assertJsonObjectEquals(step4, jsonAddABCD);
    }

    @Test
    public void testConvertAllDataToId() {
        String converted = MinderJsonPatchUtil.convertChildrenArrayToObject(jsonAddABCD);
        String reverted = MinderJsonPatchUtil.convertChildrenObjectToArray(converted);

        System.out.println("childrenArray 转为 jsonObject 后 " + converted);
        System.out.println("childrenObject 转回 childrenArray 后" + reverted);

        assertJsonObjectEquals(jsonAddABCD, reverted);
    }

    @Test // 测试当转换时遇到 order 比 childrenObject 的子元素个数大时，元素依然能被转换，不被丢失或者遗漏转换
    public void testConvertChildrenObjectToChildrenArrayWithOrderBiggerThanChildrenObjectKeysetSize() throws IOException {
        String jsonWithChildrenObject = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonWithChildrenObject.json")), "utf-8");
        String jsonWithChildrenArray = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonWithChildrenArray.json")), "utf-8");

        assertJsonObjectEquals(jsonWithChildrenArray, MinderJsonPatchUtil.convertChildrenObjectToArray(jsonWithChildrenObject));
    }

    @Test
    public void testReplaceWouldCheckBaseData() throws IOException {
        // 用户 a 把 a 的 text 属性从 A 改为了 AA
        String replaceAtoAA = MinderJsonPatchUtil.getContentPatch(jsonAddNodeA, jsonChangeNodeAToAA);
        String latestContent = MinderJsonPatchUtil.batchApplyPatch(replaceAtoAA, jsonAddNodeA).getJsonAfterPatch();

        // 另一个用户，把 a 的 text 从 A 改为了 AAA
        String replaceAto3A = MinderJsonPatchUtil.getContentPatch(jsonAddNodeA, jsonChangeNodeAToAAA);
        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(replaceAto3A, latestContent);

        // 此时应该会发现有冲突，因为第二个用户的变更基于的原始值不对
        Assert.assertNotEquals(0, applyPatchResultDto.getConflictPatch().size());
        assertJsonArrayEquals("[{'op':'test','path':'/root/childrenObject/cby3dozxagw0/data/text','value':'A'},{'op':'replace','fromValue':'A','path':'/root/childrenObject/cby3dozxagw0/data/text','value':'AAA'}]", applyPatchResultDto.getConflictPatch().get(0));

        // 用户更新了内容，从 a 的 text 从 AA 改为 AAA
        String replace2Ato3A = MinderJsonPatchUtil.getContentPatch(jsonChangeNodeAToAA, jsonChangeNodeAToAAA);
        applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(replace2Ato3A, latestContent);

        // 这次可以正常应用了
        Assert.assertEquals(0, applyPatchResultDto.getConflictPatch().size());
    }

    @Test
    public void testRemoveWouldCheckBaseDataInRemoveNotNode() throws IOException {
        // 用户 a 给 A 删除了优先级
        String baseJson = jsonAddPriorityOnABaseAD;
        String deleteExpandStateInA = MinderJsonPatchUtil.getContentPatch(baseJson, jsonAddNodeDBaseNodeA);

        // 可以删除成功
        String latestContent = MinderJsonPatchUtil.batchApplyPatch(deleteExpandStateInA, baseJson).getJsonAfterPatch();
        assertJsonObjectEquals(jsonAddNodeDBaseNodeA, latestContent);
    }

    @Test
    public void testRemoveWouldCheckBaseDataInRemoveNodeChildrenObjectConflict() throws IOException {
        // 用户 a 给 A 添加了 D 节点
        String baseJson = jsonAddNodeA;

        String addDtoA = MinderJsonPatchUtil.getContentPatch(baseJson, jsonAddNodeDBaseNodeA);
        String latestContent = MinderJsonPatchUtil.batchApplyPatch(addDtoA, baseJson).getJsonAfterPatch();

        assertJsonObjectEquals(jsonAddNodeDBaseNodeA, latestContent);

        // 另一个用户，把 A 删掉了，此时他还没看到 D 节点
        String deleteA = MinderJsonPatchUtil.getContentPatch(baseJson, jsonNoNode);
        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(deleteA, latestContent);

        // 应该会存在冲突
        Assert.assertNotEquals(0, applyPatchResultDto.getConflictPatch().size());
        assertJsonArrayEquals("[{'op':'test','path':'/root/childrenObject/cby3dozxagw0/data','value':{'created':1623140763953,'id':'cby3dozxagw0','text':'A'}},{'op':'test','path':'/root/childrenObject/cby3dozxagw0/childrenObject','value':{}},{'op':'remove','path':'/root/childrenObject/cby3dozxagw0','value':{'data':{'created':1623140763953,'id':'cby3dozxagw0','text':'A'},'childrenObject':{},'order':0}}]",
                applyPatchResultDto.getConflictPatch().get(0));

        // 用户更新了内容，再删除 A
        String deleteAwithD = MinderJsonPatchUtil.getContentPatch(latestContent, jsonNoNode);
        applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(deleteAwithD, latestContent);
        latestContent = applyPatchResultDto.getJsonAfterPatch();

        // 不再有冲突
        Assert.assertEquals(0, applyPatchResultDto.getConflictPatch().size());
        assertJsonObjectEquals(jsonNoNode, latestContent);
    }

    @Test // 删除的是节点，校验时只校验 data 和 childrenObject ，忽略 order 的校验。主要是针对在测试任务中，order 不一定和全集一致的情况。
    public void testRemoveWouldCheckBaseDataInRemoveNodeOrderConflict() throws IOException {
        // 用户在子集中只看到 B 节点和其子节点 C ，进行了删除
        String deleteB = MinderJsonPatchUtil.getContentPatch(jsonAddNodeCBaseNodeB, jsonNoNode);

        // 实际全集里，B 是在 A 同级的下一个的。应用变更时应该成功
        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(deleteB, jsonAddABCD);

        // 此时，不应该出现冲突
        Assert.assertEquals(0, applyPatchResultDto.getConflictPatch().size());
        //  删除后，只剩下 AD 节点
        assertJsonObjectEquals(jsonAddNodeDBaseNodeA, applyPatchResultDto.getJsonAfterPatch());
    }

    @Test
    public void testMoveNodeShouldNotBeRemoveAndAdd() throws IOException {
        String moveBBaseAOnABCDPatch = MinderJsonPatchUtil.getContentPatch(jsonAddABCD, jsonMoveBBaseAOnABCD);

        // 确认只生成了 move 操作。后面的 order 是为了修复测试任务 order 不一定准确的问题的，不影响本身 move 合并
        assertJsonArrayEquals("[{'op':'move','path':'/root/childrenObject/cby3dozxagw0/childrenObject/cby3h1dtg4g0','from':'/root/childrenObject/cby3h1dtg4g0'},{'op':'replace','path':'/root/childrenObject/cby3dozxagw0/childrenObject/cby3h1dtg4g0/order','value':1}]",
                moveBBaseAOnABCDPatch);

        // 确认 move 操作应用后的结果，和原来生成 patch 的结果完全一致
        ApplyPatchResultDto afterPatch = MinderJsonPatchUtil.batchApplyPatch(moveBBaseAOnABCDPatch, jsonAddABCD);
        assertJsonObjectEquals(jsonMoveBBaseAOnABCD, afterPatch.getJsonAfterPatch());
    }

    @Test
    public void testMoveAndChangeOrder() throws IOException {
        String moveBBaseAAfterDOnABCDPatch = MinderJsonPatchUtil.getContentPatch(jsonAddABCD, jsonMoveBBaseABeforeDOnABCD);

        // 确认有把 B 的 order 从 1 变为 0 的操作
        System.out.println("moveBBaseAAfterDOnABCDPatch: " + moveBBaseAAfterDOnABCDPatch);

        // 确认操作后的结果，和原来生成的 patch 结果完全一致
        ApplyPatchResultDto afterPatch = MinderJsonPatchUtil.batchApplyPatch(moveBBaseAAfterDOnABCDPatch, jsonAddABCD);
        assertJsonObjectEquals(jsonMoveBBaseABeforeDOnABCD, afterPatch.getJsonAfterPatch());
    }

    @Test
    public void testIgnoreOrderConflict() throws IOException {
        // 场景：根节点下第一级别，依次有 A、B、E 三个节点。经过筛选，只展示了 A、E 节点，这时候往 A、E 节点中间插入 F ，应该不出现冲突，且确认 F 插入成功，位置确实在 A、E 之间
        String json1LevelABE = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/json1LevelABE.json")), "utf-8");
        String json1LevelAE = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/json1LevelAE.json")), "utf-8");
        String json1LevelAFE = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/json1LevelAFE.json")), "utf-8");

        // 在测试任务中，只看到 AE ，并在 A和E中间插入了 F
        String addFBetweenAE = MinderJsonPatchUtil.getContentPatch(json1LevelAE, json1LevelAFE);

        // 但实际完整的用例已有 ABE ，所以插入 F 理论上会引起冲突，需要加忽略 order 冲突的 flag
        ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(addFBetweenAE, json1LevelABE, EnumSet.of(IGNORE_REPLACE_ORDER_CONFLICT));

        // 确认没产生冲突，忽略了 order replace 的冲突
        Assert.assertEquals(0, applyPatchResultDto.getConflictPatch().size());

        // 确认最终 F 被加到了 A、E 之间
        String latestContent = applyPatchResultDto.getJsonAfterPatch();
        JsonNode latestContentObject = objectMapper.readTree(latestContent);
        ArrayNode nodesUnderRoot = (ArrayNode) latestContentObject.get("root").get("children");
        assertJsonArrayEquals("[{'data':{'created':1623140763953,'id':'cby3dozxagw0','text':'A'},'children':[]},{'data':{'created':1623141026007,'id':'cby3h1dtg4g0','text':'B'},'children':[]},{'data':{'created':1623141026022,'id':'cby3h1dtg422','text':'F'},'children':[]},{'data':{'created':1623141026011,'id':'cby3h1dtg411','text':'E'},'children':[]}]\n",
                objectMapper.writeValueAsString(nodesUnderRoot));
    }

    @Test
    public void testIgnoreExpandStateConflict() throws IOException {
        // 场景：可能在浏览过程中无意收起了其他的节点有改动过的节点，造成冲突。此类冲突应该忽略，不应该反馈出来

        // 某用户把节点展开了
        String latestConetent = jsonNodeAExpand;

        // 另一个用户，打开时节点是非展开的，手动将其展开
        String expandPatch = MinderJsonPatchUtil.getContentPatch(jsonNodeACollapse, jsonNodeAExpand);

        // 确认不会产生冲突
        Assert.assertEquals(0, MinderJsonPatchUtil.batchApplyPatch(expandPatch, latestConetent, EnumSet.of(IGNORE_EXPAND_STATE_CONFLICT)).getConflictPatch().size());
    }

    @Test
    public void testMarkAddNode() throws IOException {
        // 新增（需递归给增加的内容添加绿色）
        String addNodeAD = MinderJsonPatchUtil.getContentPatch(jsonNoNode, jsonAddNodeDBaseNodeA);
        String markAdd = MinderJsonPatchUtil.markJsonPatchOnMinderContent(addNodeAD, jsonAddNodeDBaseNodeA);

        System.out.println("markedAdd: " + markAdd);
        String addPath = objectMapper.readTree(addNodeAD).get(0).get("path").asText();
        JsonNode markAddJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markAdd));
        JsonNode addNode = markAddJson.at(addPath);
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#67c23a','id':'cby3dozxagw0','text':'A'},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'background':'#67c23a','id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(addNode));
    }
    @Test
    public void testMarkAddAttr() throws IOException {
        // 新增有可能只是属性的新增（比如优先级、自定义标签），此时标记为修改节点，并只标记此节点。
        String addAttributeOnA = MinderJsonPatchUtil.getContentPatch(jsonAddNodeDBaseNodeA, jsonAddPriorityOnABaseAD);
        String markAddAttribute = MinderJsonPatchUtil.markJsonPatchOnMinderContent(addAttributeOnA, jsonAddPriorityOnABaseAD);

        System.out.println("markAddAttribute " + markAddAttribute);
        String addAttributePath = objectMapper.readTree(addAttributeOnA).get(0).get("path").asText();
        String addAttributeNodePath = addAttributePath.substring(0, addAttributePath.lastIndexOf("/data"));
        JsonNode markAddAttributeJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markAddAttribute));
        JsonNode addAttributeNode = markAddAttributeJson.at(addAttributeNodePath);
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#409eff','id':'cby3dozxagw0','text':'A','priority':1},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(addAttributeNode));
    }

    @Test
    public void testMarkRemoveNodeNotInConflict() throws IOException {
        // 删除1：应用成功的场景（实际新内容已经没有被删除的节点了）
        String deleteNodeAD = MinderJsonPatchUtil.getContentPatch(jsonAddNodeDBaseNodeA, jsonNoNode);
        String markRemove = MinderJsonPatchUtil.markJsonPatchOnMinderContent(deleteNodeAD, jsonNoNode);

        System.out.println("markedRemove: " + markRemove);
        // remove 的前面会加 test 操作，所以要取最后一个
        JsonNode deleteNodeADPatch = objectMapper.readTree(deleteNodeAD).get(0);
        String removePath = deleteNodeADPatch.get(deleteNodeADPatch.size()-1).get("path").asText();
        JsonNode markRemoveJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markRemove));
        JsonNode removeNode = markRemoveJson.at(removePath);
        // 把被删除的节点内容补充回来了
        Assert.assertFalse(removeNode.isMissingNode());
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#f56c6c','id':'cby3dozxagw0','text':'A'},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'background':'#f56c6c','id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(removeNode));

    }

    @Test
    public void testMarkRemoveNodeInConflict() throws IOException {
        // 删除2：冲突的场景（实际新内容里还是有被删除节点）
        String deleteNodeAD = MinderJsonPatchUtil.getContentPatch(jsonAddNodeDBaseNodeA, jsonNoNode);
        JsonNode deleteNodeADPatch = objectMapper.readTree(deleteNodeAD).get(0);
        String removePath = deleteNodeADPatch.get(deleteNodeADPatch.size() - 1).get("path").asText();
        String markRemoveInConflict = MinderJsonPatchUtil.markJsonPatchOnMinderContent(deleteNodeAD, jsonAddNodeDBaseNodeA);

        System.out.println("markRemoveInConflict: " + markRemoveInConflict);
        JsonNode markRemoveInConflictJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markRemoveInConflict));
        JsonNode removeInConflictNode = markRemoveInConflictJson.at(removePath);
        Assert.assertFalse(removeInConflictNode.isMissingNode());
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#f56c6c','id':'cby3dozxagw0','text':'A'},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'background':'#f56c6c','id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(removeInConflictNode));
    }

    @Test
    public void testMarkRemoveAttr() throws IOException {
        String deletePriorityOnA = MinderJsonPatchUtil.getContentPatch(jsonAddPriorityOnABaseAD, jsonAddNodeDBaseNodeA);
        String markRemoveAttr = MinderJsonPatchUtil.markJsonPatchOnMinderContent(deletePriorityOnA, jsonAddABCD);

        JsonNode markRemoveAttrJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markRemoveAttr));
        String removeAttrPath = objectMapper.readTree(deletePriorityOnA).get(0).get(1).get("path").asText();
        String removeAttrNodePath = removeAttrPath.substring(0, removeAttrPath.lastIndexOf("/data"));
        JsonNode removeAttrNode = markRemoveAttrJson.at(removeAttrNodePath);
        Assert.assertFalse(removeAttrNode.isMissingNode());
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#409eff','id':'cby3dozxagw0','text':'A'},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(removeAttrNode));
    }

    @Test
    public void testMarkReplaceAttr() throws IOException {
        // 修改: replace （只针对单节点进行）
        String replaceAAtoA = MinderJsonPatchUtil.getContentPatch(jsonChangeNodeAToAA, jsonAddNodeA);
        String markReplace = MinderJsonPatchUtil.markJsonPatchOnMinderContent(replaceAAtoA, jsonAddNodeDBaseNodeA);

        System.out.println("markReplace: " + markReplace);
        JsonNode markReplaceJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markReplace));
        // replace 的前面会加 test 操作，所以 get(0) 后要再 get(1)
        String replaceKeyPath = objectMapper.readTree(replaceAAtoA).get(0).get(1).get("path").asText();
        String replaceNodePath = replaceKeyPath.substring(0, replaceKeyPath.lastIndexOf("/data"));
        JsonNode replaceNode = markReplaceJson.at(replaceNodePath);
        // replace 只会标记修改当前的节点，不会动子节点
        assertJsonObjectEquals("{'data':{'created':1623140763953,'background':'#409eff','id':'cby3dozxagw0','text':'A'},'childrenObject':{'cby3h1dtg4ff':{'data':{'created':1623141026009,'id':'cby3h1dtg4ff','text':'D'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(replaceNode));

    }

    @Test
    public void testMarkMoveNode() throws IOException {
        // 修改: move （需递归给 move 结果节点添加蓝色）
        String moveBBaseAOnABCDPatch = MinderJsonPatchUtil.getContentPatch(jsonAddABCD, jsonMoveBBaseAOnABCD);
        String markMove = MinderJsonPatchUtil.markJsonPatchOnMinderContent(moveBBaseAOnABCDPatch, jsonMoveBBaseABeforeDOnABCD);

        System.out.println("markMove: " + markMove);
        JsonNode markMoveJson = objectMapper.readTree(MinderJsonPatchUtil.convertChildrenArrayToObject(markMove));
        String movePath = objectMapper.readTree(moveBBaseAOnABCDPatch).get(0).get("path").asText();
        JsonNode moveNode = markMoveJson.at(movePath);
        assertJsonObjectEquals("{'data':{'created':1623141026007,'background':'#409eff','id':'cby3h1dtg4g0','text':'B'},'childrenObject':{'cby3h1dtg4g0':{'data':{'created':1623141026007,'background':'#409eff','id':'cby3h1dtg4g0','text':'C'},'childrenObject':{},'order':0}},'order':0}",
                objectMapper.writeValueAsString(moveNode));
    }

    @Test
    public void testCleanAllProgress() throws IOException {
        String jsonWithProgress = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonWithProgress.json")), "utf-8");
        String jsonWithNoProgress = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource("MinderJsonPatchUtilTest/jsonWithNoProgress.json")), "utf-8");

        String jsonAfterCleanProgress = MinderJsonPatchUtil.cleanAllProgress(jsonWithProgress);

        Assert.assertEquals(JSONObject.parseObject(jsonAfterCleanProgress), JSON.parseObject(jsonWithNoProgress));
    }


    private void assertJsonObjectEquals(String expected, String actual) {
        Assert.assertEquals(JSONObject.parseObject(expected), JSONObject.parseObject(actual));
    }

    private void assertJsonArrayEquals(String expected, String actual) {
        Assert.assertEquals(JSONArray.parseArray(expected), JSONArray.parseArray(actual));
    }
}

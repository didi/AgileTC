package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.ProgressEnum;
import com.xiaoju.framework.entity.xmind.*;
import org.apache.commons.collections4.CollectionUtils;
import org.xmind.core.ITopic;
import org.xmind.core.IWorkbook;
import org.xmind.core.marker.IMarkerRef;

import java.util.*;

/**
 * 树 - 数据结构处理类
 * xmind 和 文件夹都用到了
 *
 * @author didi
 * @date 2020/11/26
 */
public class TreeUtil {

    // 剥离出progress的内容
    public static JSONObject parse(String caseContent) {
        JSONObject retContent = new JSONObject();
        CaseContent content = JSONObject.parseObject(caseContent, CaseContent.class);
        CaseCount count = caseDFS(content.getRoot());
        JSONObject caseObj = JSON.parseObject(caseContent);

        retContent.put("progress", count.getProgress());
        retContent.put("content", caseObj);
        retContent.put("passCount", count.getPassCount());
        retContent.put("totalCount", count.getTotal());
        retContent.put("successCount", count.getSuccess());
        retContent.put("blockCount", count.getBlock());
        retContent.put("failCount", count.getFail());
        retContent.put("ignoreCount", count.getIgnore());

        return retContent;
    }

    /**
     * ******深度递归，获取每个用例的具体内容，读出所有的计数**********
     * 根据一份测试用例，递归获取其中所有底部节点的用例执行情况
     * 分为两种情况：
     * ①当前节点有子节点
     *  <1>如果当前节点状态为1、5、9，那么值收集下游节点的个数total和，然后变成自己对应的状态个数+=childTotalSum,total++
     *  <2>如果节点为4，则忽略
     *  <3>如果节点为null，则total++
     * ②当前节点无子节点
     *  计数体对应的状态数++,total++
     *
     * @param rootData 当前用例节点
     * @return 记录对象体
     */
    private static CaseCount caseDFS(RootData rootData) {
        CaseCount currCount = new CaseCount();

        DataObj currNode = rootData.getData();
        List<RootData> childNodes = rootData.getChildren();

        if (!CollectionUtils.isEmpty(childNodes)) {

            if (currNode.getProgress() != null) {
                int num = 0;
                for (RootData childNode : childNodes) {
                    CaseCount cc = caseDFS(childNode);
                    num += cc.getTotal();
                    currCount.addAllProgress(cc.getProgress());
                }
                switch (ProgressEnum.findEnumByProgress(currNode.getProgress())) {
                    case BLOCK:
                        currCount.combineBlock(num);
                        currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                        break;
                    case SUCCESS:
                        currCount.combineSuccess(num);
                        currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                        break;
                    case FAIL:
                        currCount.combineFail(num);
                        currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                        break;
                    case IGNORE:
                        currCount.combineIgnore(num);
                        currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                        break;
                    default:
                        currCount.addTotal(num);
                }
            } else {
                for (RootData childNode : childNodes) {
                    currCount.cover(caseDFS(childNode));
                }
            }
        } else {
            // 最底部的节点，没有任何子节点
            switch (ProgressEnum.findEnumByProgress(currNode.getProgress())) {
                case BLOCK:
                    currCount.addBlock();
                    currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                    break;
                case SUCCESS:
                    currCount.addSuccess();
                    currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                    break;
                case FAIL:
                    currCount.addFail();
                    currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                    break;
                case IGNORE:
                    currCount.addIgnore();
                    currCount.addProgress(currNode.getId(), currNode.getProgressStr());
                    break;
                default:
                    currCount.addTotal();
            }
        }
        return currCount;
    }

    // 获取指定优先级的内容，入参为root节点
    public static void getPriority(Stack<JSONObject> stackCheck, Stack<IntCount> iCheck, JSONObject parent, List<String> priorities) {
        JSONArray children = parent.getJSONArray("children");
        IntCount i = new IntCount(0);

        for (; i.get() < children.size(); ) {

            JSONObject obj = (JSONObject) children.get(i.get());
            i.add();
            if (obj.getJSONObject("data").containsKey("priority") && isPriorityIn(obj.getJSONObject("data").getInteger("priority"), priorities)) {
                continue;
            } else {
                if (obj.getJSONArray("children").size() == 0) { // 当前是叶子结点
                    children.remove(obj);
                    i.del();
                    traverseCut(stackCheck, iCheck);
                } else {
                    stackCheck.push(obj);
                    iCheck.push(i);
                    getPriority(stackCheck, iCheck, obj, priorities);
                }
            }
        }
    }

    //获取指定标签case
    public static boolean getChosenCase(JSONObject root, Set<String> tags, String field) {
        if (root == null) return false;

        boolean hasTags = false;
        //筛选标签
        if (field.equals("resource")) {
            JSONArray objects = root.getJSONObject("data").getJSONArray("resource");
            if (objects != null) {
                for (Object o : objects) {
                    hasTags = hasTags || tags.contains(o);
                }
            }
            if (hasTags) return true;
        } else if (field.equals("priority")) { //筛选优先级
            String priority = root.getJSONObject("data").getString("priority");
            if (tags.contains(priority)) return true;
        }
        JSONArray children = root.getJSONArray("children");
        Iterator<Object> iterator = children.iterator();
        while (iterator.hasNext()) {
            JSONObject child = (JSONObject) iterator.next();
            if (!getChosenCase(child, tags, field)) iterator.remove();
        }
        return children.size() != 0;

    }

    //获取节点个数以及标签信息
    public static Integer getCaseNum(JSONObject root, Set<String> set) {
        if (root == null) return 0;
        int res = 0;

        JSONArray resource = root.getJSONObject("data").getJSONArray("resource");

        if (resource != null) {
            for (Object o : resource) {
                set.add((String) o);
            }
        }

        JSONArray children = root.getJSONArray("children");
        if(children.size() == 0) return 1;
        for (Object child : children) {
            res += getCaseNum((JSONObject) child, set);
        }


        return res;
    }


    //获取优先级为0的内容，入参为root节点
    public static void getPriority0(Stack<JSONObject> stackCheck, Stack<IntCount> iCheck, JSONObject parent) {
        JSONArray children = parent.getJSONArray("children");
        IntCount i = new IntCount(0);

        for (; i.get() < children.size(); ) {

            JSONObject obj = (JSONObject) children.get(i.get());
            i.add();
            if (obj.getJSONObject("data").containsKey("priority") && obj.getJSONObject("data").getLong("priority") == 1L) {
                continue;
            } else {
                if (obj.getJSONArray("children").size() == 0) { // 当前是叶子结点
                    children.remove(obj);
                    i.del();
                    traverseCut(stackCheck, iCheck);
                } else {
                    stackCheck.push(obj);
                    iCheck.push(i);
                    getPriority0(stackCheck, iCheck, obj);
                }
            }
        }
    }

    static boolean isPriorityIn(Integer data, List<String> priorities) {
        for (String priority : priorities) {
            if (data == Integer.parseInt(priority)) {
                return true;
            }
        }
        return false;
    }

    public static void traverseCut(Stack<JSONObject> stackCheck, Stack<IntCount> iCheck) {
        int size = stackCheck.size();
        for (int i = 0; i < size - 1; i++) {
            JSONObject top = stackCheck.peek();
            if (top.getJSONArray("children").size() == 0) {
                stackCheck.pop();
                stackCheck.peek().getJSONArray("children").remove(top);
                iCheck.pop().del();
            } else {
                break;
            }
        }
    }

    /**
     * 将执行结果合并到用例中
     *
     * @param caseContent 用例内容
     * @param execContent 执行内容
     */
    public static void mergeExecRecord(JSONObject caseContent, JSONObject execContent, IntCount execCount) {
        String srcId = caseContent.getJSONObject("data").getString("id");
        if (execContent.containsKey(srcId)) {
            caseContent.getJSONObject("data").put("progress", execContent.getLong(srcId));
            execCount.del();
        }
        for (Object o : caseContent.getJSONArray("children")) {
            if (!(execCount.get() == 0)) {
                mergeExecRecord(((JSONObject) o), execContent, execCount);
            }
        }
    }

    // 导出内容到xmind
    public static void exportData(JSONArray children, IWorkbook workbook, ITopic rootTopic) {
        for (Object o : children) {
            JSONObject dataObj = ((JSONObject) o).getJSONObject("data");
            ITopic topic = workbook.createTopic();
            topic.setTitleText(dataObj.getString("text"));
            topic.setFolded(true);
            rootTopic.add(topic, ITopic.ATTACHED);
            if (((JSONObject) o).getJSONArray("children").size() > 0) {
                exportData(((JSONObject) o).getJSONArray("children"), workbook, topic);
            }
        }
    }

    // 导入xmind内容
    public static void importData(JSONArray children, ITopic iTopic) {
        JSONObject rootObj = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray childrenNext = new JSONArray();
        dataObj.put("text", iTopic.getTitleText());
        dataObj.put("created", iTopic.getModifiedTime());
        dataObj.put("id", iTopic.getId());

        Map<String, Integer> priorityIds = new HashMap<>();
        priorityIds.put("priority-1", 1);
        priorityIds.put("priority-2", 2);
        priorityIds.put("priority-3", 3);
        priorityIds.put("priority-4", 3);
        priorityIds.put("priority-5", 3);
        priorityIds.put("priority-6", 3);
        priorityIds.put("priority-7", 3);
        priorityIds.put("priority-8", 3);
        priorityIds.put("priority-9", 3);
        Set<IMarkerRef> markerRefs = iTopic.getMarkerRefs();
        if (markerRefs != null && markerRefs.size() > 0) {
            for (IMarkerRef markerRef : markerRefs) {
                String markerId = markerRef.getMarkerId();
                if (priorityIds.containsKey(markerId)) {
                    dataObj.put("priority", priorityIds.get(markerId));
                }
            }
        }
        rootObj.put("data", dataObj);
        rootObj.put("children", childrenNext);
        if (children != null) {
            children.add(rootObj);
        }
        for (ITopic topic : iTopic.getAllChildren()) {
            importData(childrenNext, topic);
        }

    }

    //根据xmind解压的json文件导入xmind内容
    public static void importDataByJson(JSONArray children, JSONObject rootTopic) {
        JSONObject rootObj = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray childrenNext = new JSONArray();
        dataObj.put("text", rootTopic.getString("title"));
        dataObj.put("created", System.currentTimeMillis());
        dataObj.put("id", rootTopic.getString("id"));

        Map<String, Integer> priorityIds = new HashMap<>();
        priorityIds.put("priority-1", 1);
        priorityIds.put("priority-2", 2);
        priorityIds.put("priority-3", 3);
        priorityIds.put("priority-4", 3);
        priorityIds.put("priority-5", 3);
        priorityIds.put("priority-6", 3);
        priorityIds.put("priority-7", 3);
        priorityIds.put("priority-8", 3);
        priorityIds.put("priority-9", 3);
        JSONArray markers = rootTopic.getJSONArray("markers");
        if (markers != null && markers.size() > 0) {
            for (int i = 0; i < markers.size(); i++) {
                String markerId = markers.getJSONObject(i).getString("markerId");
                if (priorityIds.containsKey(markerId)) {
                    dataObj.put("priority", priorityIds.get(markerId));
                }
            }
        }
        rootObj.put("data", dataObj);
        rootObj.put("children", childrenNext);
        if (children != null) {
            children.add(rootObj);
        }
        if (rootTopic.containsKey("children") && rootTopic.getJSONObject("children").containsKey("attached")) {
            JSONArray jsonArray = rootTopic.getJSONObject("children").getJSONArray("attached");
            for (int i = 0; i < jsonArray.size(); i++) {
                importDataByJson(childrenNext, (JSONObject) jsonArray.get(i));
            }
        }
    }

}

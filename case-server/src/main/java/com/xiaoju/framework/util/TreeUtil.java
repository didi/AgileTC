package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.ProgressEnum;
import com.xiaoju.framework.entity.xmind.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.dom4j.Element;
import org.springframework.util.StringUtils;


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
            // 先把超链接、备注都加进来
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
            if (obj.getJSONObject("data").containsKey("priority") &&
                    obj.getJSONObject("data").getInteger("priority") != null &&
                    isPriorityIn(obj.getJSONObject("data").getInteger("priority"), priorities)) {
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
            if (data != null && data.equals(Integer.parseInt(priority))) {
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
            if (execCount.get() != 0) {
                mergeExecRecord(((JSONObject) o), execContent, execCount);
            }
        }
    }

    // 导出json内容到xml
    public static void exportDataToXml(JSONArray children, Element rootTopic) {
        if(children.size() == 0)
            return;
        Element children1 = rootTopic.addElement("children");
        Element  topics = children1.addElement("topics").addAttribute("type","attached");
        for (Object o : children) {
            JSONObject dataObj = ((JSONObject) o).getJSONObject("data");
            Element topic = topics.addElement("topic")
                    .addAttribute("id",dataObj.getString("id"))
                    .addAttribute("modified-by","didi")
                    .addAttribute("timestamp",dataObj.getString("created"));
            Element title = topic.addElement("title");
            String text = dataObj.getString("text");
            if (!StringUtils.isEmpty(text)) {
                text = StringEscapeUtils.escapeXml11(text);
            } else {
                text = "";
            }
            title.setText(text);

            String priority = getPriorityByJson(dataObj);
            if(priority != null && !priority.equals("")){
                Element marker_refs  = topic.addElement("marker-refs");
                marker_refs.addElement("marker-ref")
                        .addAttribute("marker-id",priority);
            }
            if (((JSONObject) o).getJSONArray("children").size() > 0) {
                exportDataToXml(((JSONObject) o).getJSONArray("children"), topic);
            }
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

        Integer priority = getPriorityByJsonArray(rootTopic.getJSONArray("markers"));

        if(priority != 0)
        {
            dataObj.put("priority",priority);
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

    //导入xml内容
     public  static JSONArray importDataByXml(Element e) {
         JSONArray jsonArray = new JSONArray();
         List<Element> elementList = e.elements();
         if(elementList.size() == 0)
             return jsonArray;
         for(Element element1:elementList)
         {
             if(element1.getName().equalsIgnoreCase("topic"))
             {
                 JSONArray childrenNext = new JSONArray();
                 JSONObject root = new JSONObject();
                 JSONObject dataObj = new JSONObject();
                 List<Element> newList = element1.elements();
                 String text = "";
                 Integer priorityId = 0;
                 String created = element1.attributeValue("timestamp");
                 String id = element1.attributeValue("id");

                 for (Element element : newList) {
                     if (element.getName().equalsIgnoreCase("title")) {
                         //标题
                         text = element.getText();
                     }else if (element.getName().equalsIgnoreCase("marker-refs")) {
                         // 优先级
                         priorityId =  getPriorityByElement(element);
                     }else if (element.getName().equalsIgnoreCase("children")) {
                         //子节点
                         List<Element> elementList1 = element.elements();
                         for(Element childEle:elementList1)
                         {
                             if(childEle.getName().equalsIgnoreCase("topics"))
                             {
                                 JSONArray jsonArray1 = importDataByXml(childEle);
                                 if(jsonArray1.size()>0){
                                     childrenNext.addAll(jsonArray1);
                                 }
                             }
                         }
                     } else {
                         continue;
                     }
                 }

                 dataObj.put("created", created);
                 dataObj.put("id", id);
                 dataObj.put("text", text);
                 dataObj.put("priority", priorityId);
                 root.put("data",dataObj);
                 if(childrenNext.size() != 0) {
                     root.put("children",childrenNext);
                 }
                 jsonArray.add(root);
             }
         }
         return jsonArray;

     }

     //根据xml文件获取优先级
     private static Integer getPriorityByElement(Element element)
     {
         Integer priorityId = 0;
         Map<String, Integer> priorityIds = getAllPriority();
         List<Element> markers = element.elements();
         if (markers != null && markers.size() > 0) {
             for (Element mark : markers) {
                 String markId = mark.attributeValue("marker-id");
                 if (priorityIds.containsKey(markId)) {
                     priorityId = priorityIds.get(markId);
                 }
             }
         }
         return priorityId;
     }

    //根据content.json文件获取优先级
    private static Integer getPriorityByJsonArray(JSONArray markers)
    {
        Integer priorityId = 0;
        Map<String, Integer> priorityIds = getAllPriority();
        if (markers != null && markers.size() > 0) {
            for (int i = 0; i < markers.size(); i++) {
                String markerId = markers.getJSONObject(i).getString("markerId");
                if (priorityIds.containsKey(markerId)) {
                    priorityId = priorityIds.get(markerId);
                }
            }
        }
        return priorityId;
    }


    //根据case-server  json获取xml优先级
    private static String getPriorityByJson(JSONObject jsonObject)
    {
        Integer priority = 0;
        priority = jsonObject.getInteger("priority");
        String topicPriority = "";
        if(priority != null && priority != 0){
            if(priority.equals(3)){
                topicPriority = "priority-3";
            }else
            {
                Map<String, Integer> priorityIds = getAllPriority();
                for (Map.Entry<String, Integer> entry : priorityIds.entrySet()) {
                    //如果value和key对应的value相同 并且 key不在list中
                    if(priority.equals(entry.getValue())){
                        topicPriority=entry.getKey();
                        break;
                    }
                }
            }
        }
        return  topicPriority;
    }

    //获取所有优先级
     private static Map<String, Integer> getAllPriority(){
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
         return priorityIds;
     }

}

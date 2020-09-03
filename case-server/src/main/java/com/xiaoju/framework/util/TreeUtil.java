package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.xmind.core.ITopic;
import org.xmind.core.IWorkbook;
import org.xmind.core.marker.IMarkerRef;

import java.util.*;

/**
 * Created by didi on 2019/10/10.
 */
@Slf4j
public class TreeUtil {

    // 剥离出progress的内容
    public static JSONObject parse(String caseContent) {
        JSONObject progress = new JSONObject();
        JSONObject retContent = new JSONObject();
        IntCount passCount = new IntCount(0);
        IntCount totalCount = new IntCount(0);
        IntCount successCount = new IntCount(0);
        IntCount blockCount = new IntCount(0);
        JSONObject caseObj = JSON.parseObject(caseContent);
        if (caseObj.getJSONObject("root").getJSONArray("children").size() != 0) {
            traverse(caseObj.getJSONObject("root"), progress, passCount, totalCount, successCount, blockCount, false, false, false);
        }
        retContent.put("progress", progress);
        retContent.put("content", caseObj);
        retContent.put("passCount", passCount.get());//执行个数
        retContent.put("totalCount", totalCount.get());//总用例数
        retContent.put("successCount", successCount.get());//成功个数
        retContent.put("blockCount", blockCount.get());//阻塞个数

        //log.debug("content parse："+retContent.toJSONString());
        return retContent;
    }

    // 遍历json，剥离progress内容
    public static void traverse(JSONObject caseObj, JSONObject progress, IntCount passCount, IntCount totalCount, IntCount successCount, IntCount blockCount, boolean passflag, boolean successflag, boolean blockflag) {
        if (caseObj.getJSONArray("children").size() == 0) {
            totalCount.add();
            if (passflag) {
                passCount.add();
            }
            if (blockflag) {
                blockCount.add();
            }
            if (successflag) {
                successCount.add();
            }
        }

        for (Object o : caseObj.getJSONArray("children")) {
            if (((JSONObject) o).getJSONObject("data").containsKey("progress") && ((JSONObject) o).getJSONObject("data").get("progress") != null) {
                progress.put(((JSONObject) o).getJSONObject("data").getString("id"), ((JSONObject) o).getJSONObject("data").getString("progress"));
                if (((JSONObject) o).getJSONObject("data").getString("progress").equals("9")) {//通过
                    ((JSONObject) o).getJSONObject("data").remove("progress");
                    traverse(((JSONObject) o), progress, passCount, totalCount, successCount, blockCount, true, true, false);
                } else if (((JSONObject) o).getJSONObject("data").getString("progress").equals("5")) {//阻塞
                    ((JSONObject) o).getJSONObject("data").remove("progress");
                    traverse(((JSONObject) o), progress, passCount, totalCount, successCount, blockCount, true, false, true);
                } else if (((JSONObject) o).getJSONObject("data").getString("progress").equals("1")) {//未通过1
                    ((JSONObject) o).getJSONObject("data").remove("progress");
                    traverse(((JSONObject) o), progress, passCount, totalCount, successCount, blockCount, true, false, false);
                }
            } else {
                if (passflag) {//执行过
                    traverse(((JSONObject) o), progress, passCount, totalCount, successCount, blockCount, true, successflag, blockflag);
                } else {//未执行过
                    traverse(((JSONObject) o), progress, passCount, totalCount, successCount, blockCount, false, false, false);
                }
            }
        }
    }

    //获取用例总数
    public static void getCaseNum(JSONObject caseObj, IntCount totalCount) {
        if (caseObj.getJSONArray("children").size() == 0) {
            totalCount.add();
        }

        for (Object o : caseObj.getJSONArray("children")) {
            getCaseNum(((JSONObject) o), totalCount);
        }
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

//    public static void mergeExecRecord(JSONObject caseContent, JSONObject execContent) {
//        String srcId = caseContent.getJSONObject("data").getString("id");
//        if (execContent.containsKey(srcId)) {
//            caseContent.getJSONObject("data").put("progress", execContent.getLong(srcId));
//            log.info("merge result: " + caseContent.getJSONObject("data").toJSONString().substring(0,20));
//        } else {
//            for (Object o: caseContent.getJSONArray("children")) {
//                mergeExecRecord(((JSONObject) o), execContent);
//            }
//        }
//    }

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

//    public static void main(String argc[]) {
//        String casexx = "{\"template\":\"right\",\"root\":{\"data\":{\"created\":1570860748267,\"text\":\"1.0模式中心仓维修11\",\"id\":\"6la3utf8ma7kg5v7ogjp6ldqo4\"},\"children\":[{\"data\":{\"created\":1571383198489,\"text\":\"取送车菜单111\",\"id\":\"7thvpm2hrotiunjvbrtbjbhbv3\"},\"children\":[{\"data\":{\"expandState\":\"expand\",\"created\":1570773444369,\"text\":\"任务单详情页\",\"id\":\"0lj7bpsp36cjtvl8oo0kmjnb25\",\"priority\":1},\"children\":[{\"data\":{\"created\":1570775783582,\"text\":\"操作按钮\",\"id\":\"3vcrns9ae148nftq883v82c14c\"},\"children\":[{\"data\":{\"created\":1570773444364,\"text\":\"已到店\",\"id\":\"1n1e0aba85urf50p7tel5gpmod\"},\"children\":[{\"data\":{\"created\":1570776550228,\"text\":\"开单\",\"id\":\"41b8en2c3pekojfkdk0m938s7n\"},\"children\":[{\"data\":{\"created\":1570773444364,\"text\":\"“已到店”&“返修到店”的卡片或详情页\",\"id\":\"6eenvq0j9qic0svejo77pm6hdo\"},\"children\":[{\"data\":{\"created\":1570773444364,\"text\":\"点击进入到“中心仓维修开单”页面\",\"id\":\"4r3mbhnao4569hh210ofabqik1\"},\"children\":[]}]}]}]}]}]},{\"data\":{\"created\":1570773444380,\"text\":\"中心仓同步维修入库结果\",\"id\":\"1vogjsnvdkj1695q6nikhelhsu\"},\"children\":[]},{\"data\":{\"created\":1572260203016,\"id\":\"by13q5zkcts0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572260203164,\"id\":\"by13q620wog0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1572262036165,\"id\":\"by14dk4hz1c0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262037801,\"id\":\"by14dkvkem00\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262037994,\"id\":\"by14dkyqygw0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262038175,\"id\":\"by14dl1qmlk0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262038331,\"id\":\"by14dl4bggw0\",\"text\":\"分支主题\",\"priority\":1},\"children\":[{\"data\":{\"created\":1572262040351,\"id\":\"by14dm1q4ps0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1572262040593,\"id\":\"by14dm5qhu80\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1572262040875,\"id\":\"by14dmaed6g0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1572262041174,\"id\":\"by14dmfcfhc0\",\"text\":\"分支主题\"},\"children\":[]}]}]},{\"data\":{\"created\":1572262043051,\"id\":\"by14dnae6go0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262043492,\"id\":\"by14dnhoglc0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262043661,\"id\":\"by14dnkh9s80\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1572262043824,\"id\":\"by14dnn60hc0\",\"text\":\"分支主题\"},\"children\":[]}]}]}]}]}]},\"theme\":\"fresh-green-compat\",\"version\":\"1.4.43\",\"base\":124}";
//        Stack<JSONObject> objCheck = new Stack<>();
//        JSONObject obj = JSON.parseObject(casexx);
//        objCheck.push(obj.getJSONObject("root"));
//        System.out.println("ret: " + obj.toJSONString());
//    }

}

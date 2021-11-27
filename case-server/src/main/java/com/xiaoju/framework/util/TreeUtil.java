package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.ProgressEnum;
import com.xiaoju.framework.entity.request.cases.FileImportReq;
import com.xiaoju.framework.entity.xmind.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.entity.ContentType;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 树 - 数据结构处理类
 * xmind 和 文件夹都用到了
 *
 * @author didi
 * @date 2020/11/26
 */
public class TreeUtil {

    /**
     * 常量
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TreeUtil.class);


    // 剥离出progress的内容
    public static JSONObject parse(String caseContent) {
        JSONObject retContent = new JSONObject();
        CaseContent content = JSONObject.parseObject(caseContent, CaseContent.class); // 将casecontent的内容解析为CaseContent.class对象并返回
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
    public static void exportDataToXml(JSONArray children, Element rootTopic, String path){
        if(children.size() == 0)
            return;

        Document document = rootTopic.getDocument();
        LOGGER.info("rootTopic中的内容为： " + rootTopic);
        LOGGER.info("document中的内容为：" + document);
        Element children1 = rootTopic.addElement("children");
        Element topics = children1.addElement("topics").addAttribute("type","attached");
        for (Object o : children) {
            JSONObject dataObj = ((JSONObject) o).getJSONObject("data");


            Element topic = topics.addElement("topic")
                    .addAttribute("id",dataObj.getString("id"))
                    .addAttribute("modified-by","didi")
                    .addAttribute("timestamp",dataObj.getString("created"))
                    .addAttribute("imageTitle", dataObj.getString("imageTitle"));

            JSONObject dataObj1 = dataObj.getJSONObject("imageSize");
            String picPath = dataObj.getString("image");
            if(picPath != null && picPath.length() != 0){
                String targetPath = path  + "/attachments";

                // 创建一个新的文件夹
                File file = new File(targetPath);
                if(!file.isDirectory()){
                    file.mkdir();
                }
                try{
                    String[] strs = picPath.split("/");
                    int size = strs.length;
                    String fileName = strs[size - 1];
                    LOGGER.info("picPath路径为：" + picPath);
                    LOGGER.info("outfile的内容为：" + file + "/" + fileName);

                    if(dataObj1 != null && dataObj1.getString("width") != null){
                        LOGGER.info("topic1的内容为：" + topic);
                        LOGGER.info("dataonj1中有内容, 其中width：" + dataObj1.getString("width") + "  ，height：" + dataObj1.getString("height"));
                        Element imageSize = topic.addElement("xhtml:img")
                                .addAttribute("svg:height", dataObj1.getString("height"))
                                .addAttribute("svg:width", dataObj1.getString("width"))
                                .addAttribute("xhtml:src", "xap:attachments/" + fileName);

                    }

                    FileOutputStream outFile = new FileOutputStream(file + "/" + fileName);
                    URL httpUrl=new URL(picPath);
                    HttpURLConnection conn=(HttpURLConnection) httpUrl.openConnection();
                    //以Post方式提交表单，默认get方式
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    // post方式不能使用缓存
                    conn.setUseCaches(false);
                    //连接指定的资源
                    conn.connect();
                    //获取网络输入流
                    InputStream inputStream=conn.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
                    byte b [] = new byte[1024];
                    int len = 0;
                    while((len=bis.read(b))!=-1){
                        outFile.write(b, 0, len);
                    }
                    LOGGER.info("下载完成...");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

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
                Element marker_refs = topic.addElement("marker-refs");
                marker_refs.addElement("marker-ref")
                        .addAttribute("marker-id",priority);
            }
            JSONArray childChildren = ((JSONObject) o).getJSONArray("children");
            if (childChildren != null && childChildren.size() > 0) {
                exportDataToXml(childChildren, topic, path);
            }
        }
    }

    public static void importDataByJson(JSONArray children, JSONObject rootTopic, String fileName, HttpServletRequest requests, String uploadPath) throws IOException {
        JSONObject rootObj = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray childrenNext = new JSONArray();
        dataObj.put("text", rootTopic.getString("title"));
        dataObj.put("created", System.currentTimeMillis());
        dataObj.put("id", rootTopic.getString("id"));
        if(rootTopic.containsKey("image")){ // 添加imagesize属性
            // 需要将图片传到云空间中，然后将返回的链接导入
            Map<String, String> imageSize = new HashMap<>();
            // todo: 此处直接写死的方式存在问题
            imageSize.put("width", "400");
            imageSize.put("height", "184");
            String image = "";
            String picPath = "";
            String path = rootTopic.getJSONObject("image").getString("src");
            String[] strs = path.split("/");
            int len = strs.length;
            image = strs[len-1]; // 此时image为图片所在的本地位置
            // 将文件传入到temp文件下，因此需要将文件进行转换，将file文件类型转化为MultipartFile类型，然后进行上传
            File file = new File(fileName + File.separator + image);
            FileInputStream fileInputStream = new FileInputStream(file);

            MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                    ContentType.APPLICATION_OCTET_STREAM.toString(), fileInputStream);

            // 将MultipartFile文件进行上传
            JSONObject ret = new JSONObject();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
            String format = sdf.format(new Date());
            File folder = new File(uploadPath + format);// 文件夹的名字
            if (!folder.isDirectory()) { // 如果文件夹为空，则新建文件夹
                folder.mkdirs();
            }
            // 对上传的文件重命名，避免文件重名
            String oldName = multipartFile.getOriginalFilename(); // 获取文件的名字
            String newName = UUID.randomUUID().toString()
                    + oldName.substring(oldName.lastIndexOf("."), oldName.length()); // 生成新的随机的文件名字
            File newFile = new File(folder, newName);
            LOGGER.info("newFile的名字为" + newFile);
            try {
                multipartFile.transferTo(newFile);
                // 返回上传文件的访问路径
                // request.getScheme()可获取请求的协议名，request.getServerName()可获取请求的域名，request.getServerPort()可获取请求的端口号
                String filePath = requests.getScheme() + "://" + requests.getServerName()
                        + ":" + requests.getServerPort() + "/" + format + newName;
                LOGGER.info("filepath的路径为：" + filePath);
                picPath = filePath;
                JSONArray datas = new JSONArray();
                JSONObject data = new JSONObject();
                data.put("url", filePath);
                ret.put("success", 1);
                datas.add(data);
                ret.put("data", datas);
            } catch (IOException err) {
                LOGGER.error("上传文件失败, 请重试。", err);
                ret.put("success", 0);
                ret.put("data", "");
            }
            dataObj.put("image", picPath);
            dataObj.put("imageSize", imageSize);
        }

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
                importDataByJson(childrenNext, (JSONObject) jsonArray.get(i), fileName, requests, uploadPath);
            }
        }
    }


    //导入xml内容
    public static JSONArray importDataByXml(FileImportReq request, Element e, String fileName, HttpServletRequest requests, String uploadPath) throws IOException {
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
                Map<String, String> imageSize = new HashMap<>();
                String text = "";
                String picPath = "";
                Integer priorityId = 0;
                String created = element1.attributeValue("timestamp");
                String id = element1.attributeValue("id");

                for (Element element : newList) {
                    // 获取xml里面的图片大小信息
                    if(element.getName().equalsIgnoreCase("img")){ // 添加imagesize属性
                        // 需要将图片传到云空间中，然后将返回的链接导入
                        LOGGER.info("xhtml:img可以使用，其中element中的内容为：" + element);

                        String path = element.attributeValue("src");

                        // 将文件传入到temp文件下，因此需要将文件进行转换，将file文件类型转化为MultipartFile类型，然后进行上传
                        File file = new File(fileName + path.split(":")[1]);
                        try {
                            if (StringUtils.isEmpty(element.attributeValue("width")) || StringUtils.isEmpty(element.attributeValue("height"))) {
                                BufferedImage sourceImg = ImageIO.read(new FileInputStream(file));
                                imageSize.put("width", String.valueOf(sourceImg.getWidth()));
                                imageSize.put("height", String.valueOf(sourceImg.getHeight()));
                            } else {
                                imageSize.put("width", element.attributeValue("width"));
                                imageSize.put("height", element.attributeValue("height"));
                            }

                            MultipartFile multipartFile = new MockMultipartFile(file.getName(), new FileInputStream(file));

                            String fileUrlPath = FileUtil.fileUpload(uploadPath, multipartFile);

                            // 返回上传文件的访问路径
                            // request.getScheme()可获取请求的协议名，request.getServerName()可获取请求的域名，request.getServerPort()可获取请求的端口号
                            String filePath = requests.getScheme() + "://" + requests.getServerName()
                                    + ":" + requests.getServerPort() + "/" + fileUrlPath;
                            LOGGER.info("filepath的路径为：" + filePath);
                            picPath = filePath;

                        } catch (Exception err) {
                            LOGGER.error("图片上传文件失败, 请重试。", err);
                        }
                    }

                    // 获取xml里面中的图片importDataByXml1

                    else if (element.getName().equalsIgnoreCase("title")) {
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
                                JSONArray jsonArray1 = importDataByXml(request, childEle, fileName, requests, uploadPath);
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
                dataObj.put("image", picPath);
                dataObj.put("imageSize", imageSize);
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

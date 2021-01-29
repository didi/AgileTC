package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.cases.CaseCreateReq;
import com.xiaoju.framework.entity.request.cases.FileImportReq;
import com.xiaoju.framework.entity.response.cases.ExportXmindResp;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseService;
import com.xiaoju.framework.service.FileService;
import com.xiaoju.framework.util.FileUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xiaoju.framework.constants.SystemConstant.POINT;
import static com.xiaoju.framework.constants.XmindConstant.*;

/**
 * 文件上传与导出实现类
 *
 * @author didi
 * @date 2020/10/22
 */
@Service
public class FileServiceImpl implements FileService {

    @Resource
    private CaseService caseService;

    @Resource
    private TestCaseMapper caseMapper;

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importXmindFile(FileImportReq req) throws Exception {
        String fileName = req.getFile().getOriginalFilename();

        if (!StringUtils.isEmpty(fileName)) {
            // 得到上传文件的扩展名
            String suffix = fileName.substring(fileName.lastIndexOf(POINT) + 1).toLowerCase();
            if (!suffix.equals(XMIND_SUFFIX) && !suffix.equals(ZIP_SUFFIX)) {
                throw new CaseServerException("上传的文件格式不正确", StatusCode.FILE_FORMAT_ERROR);
            }

            // 把文件放到本地
            File file = new File("");
            String filePath = "";
            try {
                filePath = file.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String desPath = filePath + TEMP_FOLDER;

            File pathFile = new File(desPath);
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            desPath = desPath + fileName;
            File dest = new File(desPath);
            Long time = System.currentTimeMillis();
            String desc = filePath + TEMP_FOLDER + fileName.split("\\.")[0] + "_" + time.toString() + "/";

            // 开始转换
            req.getFile().transferTo(dest);
            if (!FileUtil.decompressZip(desPath, desc)) {
                throw new CaseServerException("解析失败", StatusCode.FILE_IMPORT_ERROR);
            }

            // 导入用例
            File jsonFile = new File((desc + CONTENT_JSON).replace("/", File.separator));
            CaseCreateReq caseCreateReq = jsonFile.exists() ? buildCaseByJson(req, desc) : buildCaseByXml(req, desc);
            return caseService.insertOrDuplicateCase(caseCreateReq);
        }
        throw new CaseServerException("传入的文件名非法", StatusCode.FILE_IMPORT_ERROR);
    }

    @Override
    public ExportXmindResp exportXmindFile(Long id, String userAgent) throws Exception {

        ExportXmindResp resp = new ExportXmindResp();

        //将用例内容写内容入xml文件
        Map<String,String> pathMap= createXml(id);

        //压缩文件夹成xmind文件
        String filePath = pathMap.get("exportPath") + ".xmind";
        FileUtil.compressZip(pathMap.get("exportPath") ,filePath);
        // 输出
        ByteArrayOutputStream byteArrayOutputStream = outPutFile(filePath);
        resp.setFileName(pathMap.get("exportFileName"));
        resp.setData(byteArrayOutputStream.toByteArray());

        return resp;
    }

    private ByteArrayOutputStream outPutFile(String filePath){

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        try{
            InputStream fis = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            int r = 0;
            while ((r = fis.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, r);
            }
            fis.close();
            bufferedOutputStream.flush();
            IOUtils.closeQuietly(bufferedOutputStream);
            IOUtils.closeQuietly(byteArrayOutputStream);
        }
        catch (Exception e){
            e.printStackTrace();
            IOUtils.closeQuietly(bufferedOutputStream);
            IOUtils.closeQuietly(byteArrayOutputStream);
            throw new CaseServerException("导出失败", StatusCode.FILE_EXPORT_ERROR);
        }
        return byteArrayOutputStream;
    }

    //根据用例生成xml文件
    private Map<String,String> createXml(Long id)
    {
        TestCase testCase = caseMapper.selectOne(id);
        if (testCase == null || StringUtils.isEmpty(testCase.getCaseContent())) {
            throw new CaseServerException("用例不存在或者content为空", StatusCode.FILE_EXPORT_ERROR);
        }
        //写入文件内容
        Document document = createDocument(testCase);

        //将内容写入xml
        String path = writeXml(document, testCase);
        Map<String,String> pathMap = new HashMap<>();
        pathMap.put("exportPath",path);
        pathMap.put("exportFileName",testCase.getTitle() + ".xmind");
        return pathMap;
    }

    //拼接xml内容
    private Document createDocument(TestCase testCase){
        // 1、创建document对象
        Document document = DocumentHelper.createDocument();
        // 2、创建根节点root
        Element root = document.addElement("xmap-content");
        // 3、生成子节点及子节点内容
        Element sheet = root.addElement("sheet")
                .addAttribute("id",ZEN_ROOT_VERSION)
                .addAttribute("modified-by",XMIND_MODIFIED_VERSION)
                .addAttribute("theme",XMIND_THEME_VERSION)
                .addAttribute("timestamp",XMIND_CREATED_VERSION);

        JSONObject rootObj = JSON.parseObject(testCase.getCaseContent()).getJSONObject(ROOT);
        Element topic = sheet.addElement("topic")
                .addAttribute("id",rootObj.getJSONObject(DATA).getString("id"))
                .addAttribute("modified-by","didi")
                .addAttribute("timestamp",rootObj.getJSONObject(DATA).getString("created"));

        Element title = topic.addElement("title");
        String text = rootObj.getJSONObject(DATA).getString("text");
        text = text.replace("<","&lt;");
        text = text.replace(">","&gt;");
        title.setText(text);
        TreeUtil.exportDataToXml(rootObj.getJSONArray("children"), topic);
        return document;
    }

    //创建要写入的文件夹
    private String creteFolder(TestCase testCase){
        String filePath = "";
        try{
            filePath = new File("").getCanonicalPath();
        }catch (Exception e){
            e.printStackTrace();
        }
        String folderName = testCase.getTitle().replace(" ","")+ "_" + System.currentTimeMillis();
        String desPath = filePath + TEMP_FOLDER_EXPORT + folderName;
        File pathFile = new File(desPath);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        return  desPath;
    }

    private String writeXml(Document document, TestCase testCase){
        String path = creteFolder(testCase);
        // 设置生成xml的格式
        OutputFormat format = OutputFormat.createPrettyPrint();
        // 设置编码格式
        format.setEncoding("UTF-8");
        String targetPath = path  + "/content.xml";
        File xmlFile = new File(targetPath);
        try {
            XMLWriter writer = new XMLWriter(new FileOutputStream(xmlFile), format);
            // 设置是否转义，默认使用转义字符
            writer.setEscapeText(false);
            writer.write(document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("生成文件夹和content.xml失败");
        }
        return path;
    }

    private CaseCreateReq buildCaseByJson(FileImportReq request, String fileName) {
        // 开始读取文件中的json内容了
        String s = FileUtil.readJsonFile(fileName);
        JSONArray parseArray = JSONObject.parseArray(s);
        JSONObject getObj = parseArray.getJSONObject(0);
        JSONObject rootTopic = getObj.getJSONObject("rootTopic");

        // case-content设置
        JSONArray jsonArray = new JSONArray();
        TreeUtil.importDataByJson(jsonArray, rootTopic);

        return buildCaseCreateReq(request, jsonArray);
    }

    //xmind8从content文件读取用例内容
    public CaseCreateReq buildCaseByXml(FileImportReq request, String fileName) throws Exception {

        // 开始读取文件中的xml内容了
        JSONArray jsonArray = new JSONArray();
        String fileXml = "content.xml";
        fileName = (fileName + fileXml).replace("/", File.separator);
        File file = new File(fileName);
        if(!file.exists())
            throw new CaseServerException("导入失败，文件不存在", StatusCode.FILE_IMPORT_ERROR);
        SAXReader reade = new SAXReader();
        org.dom4j.Document doc = reade.read(file);
        Element rootElement = doc.getRootElement();
        List<Element> elementList = rootElement.elements();
        Element childElement = elementList.get(0);
        String eleName = childElement.getName();
        if(eleName.equalsIgnoreCase("sheet"))
        {
            jsonArray = TreeUtil.importDataByXml(childElement);
        }
        return buildCaseCreateReq(request, jsonArray);
    }


    private CaseCreateReq buildCaseCreateReq(FileImportReq request, JSONArray jsonArray) {
        // 构建content
        JSONObject caseObj = new JSONObject();
        caseObj.put(ROOT, jsonArray.get(0));
        caseObj.put(TEMPLATE, TEMPLATE_RIGHT);
        caseObj.put(THEME, THEME_DEFAULT);
        caseObj.put(VERSION, VERSION_DEFAULT);
        caseObj.put(BASE, BASE_DEFAULT);

        CaseCreateReq testCase = new CaseCreateReq();
        testCase.setProductLineId(request.getProductLineId());
        testCase.setCreator(request.getCreator());
        if(request.getRequirementId().equals("undefined")) {
            testCase.setRequirementId("");
        }else {
            testCase.setRequirementId(request.getRequirementId());
        }
        testCase.setProductLineId(request.getProductLineId());
        testCase.setDescription(request.getDescription());
        testCase.setTitle(request.getTitle());
        testCase.setCaseType(0);
        testCase.setCaseContent(caseObj.toJSONString());
        testCase.setChannel(request.getChannel());
        testCase.setBizId(request.getBizId());
        return testCase;
    }
}

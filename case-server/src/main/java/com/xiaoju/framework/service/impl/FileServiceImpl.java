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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.xmind.core.*;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;

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
            CaseCreateReq caseCreateReq = jsonFile.exists() ? buildCaseByJson(req, desc) : buildCaseByXmind(req, desc);
            return caseService.insertOrDuplicateCase(caseCreateReq);
        }
        throw new CaseServerException("传入的文件名非法", StatusCode.FILE_IMPORT_ERROR);
    }

    @Override
    public ExportXmindResp exportXmindFile(Long id, String userAgent) throws Exception {
        IWorkbookBuilder builder = Core.getWorkbookBuilder();
        IWorkbook workbook = builder.createWorkbook();
        ISheet defSheet = workbook.getPrimarySheet();
        ITopic rootTopic = defSheet.getRootTopic();

        TestCase testCase = caseMapper.selectOne(id);
        if (testCase == null || StringUtils.isEmpty(testCase.getCaseContent())) {
            throw new CaseServerException("用例不存在或者content为空", StatusCode.FILE_EXPORT_ERROR);
        }

        JSONObject rootObj = JSON.parseObject(testCase.getCaseContent()).getJSONObject(ROOT);
        rootTopic.setTitleText(rootObj.getJSONObject(DATA).getString("text"));
        rootTopic.setFolded(false);

        TreeUtil.exportData(rootObj.getJSONArray("children"), workbook, rootTopic);

        defSheet.setThemeId(XMIND_THEME_VERSION);
        defSheet.setStyleId(XMIND_THEME_VERSION);

        // http返回的文件名
        String title = testCase.getTitle() + "_" + System.currentTimeMillis() + "_u.xmind";
        String fileName = new String(title.getBytes(), StandardCharsets.UTF_8);
        workbook.save(fileName);

        // 服务器本地存有的x-mind文件
        String filePath = new File("").getCanonicalPath() + "/" + fileName;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
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

        ExportXmindResp resp = new ExportXmindResp();
        resp.setFileName(fileName);
        resp.setData(byteArrayOutputStream.toByteArray());
        return resp;
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

    public CaseCreateReq buildCaseByXmind(FileImportReq request, String fileName) throws Exception {
        IWorkbookBuilder builder = Core.getWorkbookBuilder();
        IWorkbook workbook = builder.loadFromPath(fileName);
        // 打开x-mind文件，先获取主sheet，在获取根topic
        ISheet defSheet = workbook.getPrimarySheet();
        ITopic rootTopic = defSheet.getRootTopic();

        //暂不支持x-mind zen版本
        if (ZEN_ROOT_VERSION.equals(rootTopic.getId())) {
            throw new CaseServerException("暂不支持Xmind-Zen版本", StatusCode.FILE_IMPORT_ERROR);
        }

        JSONArray jsonArray = new JSONArray();
        TreeUtil.importData(jsonArray, rootTopic);

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
        testCase.setRequirementId(request.getRequirementId());
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

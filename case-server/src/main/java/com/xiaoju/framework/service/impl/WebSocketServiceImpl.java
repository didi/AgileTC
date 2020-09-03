package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.xiaoju.framework.entity.ExecRecord;
import com.xiaoju.framework.entity.PriorityEnv;
import com.xiaoju.framework.entity.RecordNum;
import com.xiaoju.framework.entity.Response.ConditionTestCaseInfoVO;
import com.xiaoju.framework.entity.Response.TestCasePriorityInfoResp;
import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.WebSocketService;
import com.xiaoju.framework.util.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.xmind.core.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by didi on 2019/9/23.
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {
    @Autowired
    private ExecRecordMapper execRecordMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;

    @Override
    public void createTestCase(TestCase testCase) {
        try {
            if (testCase.getProductLineId() == null) {
                throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "业务线id为空");
            }
            if (!StringUtils.hasLength(testCase.getTitle())) {
                throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "case Title 不能为空");
            }
            if (testCase.getCaseType() == null) {
                throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "需求类型不能为空");
            }
            if (testCase.getId() != null) {
                log.info("复制用例,case_id:" + testCase.getId());
                TestCase orgTestCase = testCaseMapper.selectByPrimaryKey(testCase.getId());
                testCase.setCaseContent(orgTestCase.getCaseContent());
            }
            testCase.setId(null);
            testCase.setIsDelete(0);
            testCase.setModifier(testCase.getCreator());
            testCase.setExtra("");

            testCaseMapper.insert(testCase);
            log.info("case create success. id:" + testCase.getId() + "\t title:" + testCase.getTitle());
        } catch (ResponseException e) {
            log.warn("case create failed. " + e.getMsg());
            throw new ResponseException(e.getErrorCode(), e.getMsg());
        } catch (Exception e) {
            log.error("Failed to save testCaseObject ", e);
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "Failed to save testCaseObject : " + e.getMessage());
        }
    }

    @Override
    public String selectCaseById(String caseId) {
        try {
            TestCase testCase = testCaseMapper.selectByPrimaryKey(Long.valueOf(caseId));
            log.info("get case  " + caseId + ", case content " + testCase.getCaseContent());
            return testCase.getCaseContent();
        } catch (Exception e) {
            log.error("get case failed. case id " + caseId + ", error msg " + e);
            return null;
        }
    }

    @Override
    public TestCase selectByPrimaryKey(Long caseId) {
        try {
            TestCase testCase = testCaseMapper.selectByPrimaryKey(Long.valueOf(caseId));
            log.info("get case  " + caseId);
            return testCase;
        } catch (Exception e) {
            log.error("get case failed. case id " + caseId + ", error msg " + e);
            return null;
        }
    }

    @Override
    public int update(TestCase testCase) {
        int ret = -1;
        try {
            ret = testCaseMapper.updateByPrimaryKeySelective(testCase);
            log.info("update case " + testCase.getId() + "return message " + ret);
        } catch (Exception e) {
            log.error("update exception " + e);
        }
        return ret;
    }

    /* 物理删除，谨慎使用 */
    @Override
    public int delete(String caseId) {
        int ret = -1;
        try {
            ret = testCaseMapper.deleteByPrimaryKey(Long.valueOf(caseId));
            log.info("delete case " + caseId + "return msg " + ret);
        } catch (Exception e) {
            log.error("delete exception " + e);
        }
        return ret;
    }

    @Override
    public List<TestCase> getIdByRequirementId(Long requirementId) {
        if (requirementId == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "需求id不能为空");
        }

        List<TestCase> TestCaseResult = testCaseMapper.listByRequirementId(requirementId);
        for (int i = 0; i < TestCaseResult.size(); i++) {
            TestCase testCase = TestCaseResult.get(i);
            Long smkCaseId = testCase.getSmkCaseId();
            if (smkCaseId != 0) {
                String smkCaseName = testCaseMapper.selectNameById(testCase.getSmkCaseId()).getTitle();
//                testCase.setSmkCaseName(smkCaseName);
            } else {
//                testCase.setSmkCaseName("");
            }
            TestCaseResult.set(i, testCase);

        }
        return TestCaseResult;
    }

    @Override
    public PageResult<TestCase> listTestCase(Long productLineId,
                                             Integer case_type,
                                             Integer pageNum,
                                             Integer pageSize,
                                             Long id,
                                             String title,
                                             String creator,
                                             String requirement_id,
                                             String beginTime,
                                             String endTime,
                                             Integer channel) {
        Date sTime = null, eTime = null;
        if (beginTime != "" && beginTime != null) {
            SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                sTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(beginTime);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        if (endTime != "" && endTime != null) {
            SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                eTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endTime);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        PageResult<TestCase> pageResult = new PageResult<>();
        String[] requirement_ids = null;
        if (requirement_id != null && !requirement_id.trim().equals("")) {
            requirement_ids = requirement_id.split(",");
        }
        //总用例数，分页用
        pageResult.setTotal(countTestCase(productLineId, case_type, id, title, creator, requirement_ids, sTime, eTime, channel));

        int offset = (pageNum - 1) * 10;
        List<TestCase> testCaseList = testCaseMapper.listTestCaseByids(productLineId, case_type, offset, pageSize, id, title, creator, requirement_ids, sTime, eTime, channel);

//        //每个用例处理
//        for (TestCase testCase :testCaseList){
//            long start0 = System.currentTimeMillis();
//            //用例对应记录
//            Long caseId = testCase.getId();
//            List<ExecRecord> listrecord = execRecordMapper.listRecordByCaseId(caseId);
//            long cost0 = System.currentTimeMillis() - start0;
//            log.info("cost0={}",cost0);
//            testCase.setExecRecordList(listrecord);
//
//            //获取当前用例总数
//            int totalCount = TreeUtil.parse(testCase.getCaseContent()).getInteger("totalCount");
//            testCase.setTotalCount(totalCount);
//
//            //记录数
//            testCase.setRecordNum(listrecord.size());
//            testCase.setCaseContent("");
//        }
        if (testCaseList.size() > 0) {
            List<Long> caseIdList = new ArrayList<>();
            for (TestCase testCase : testCaseList) {
                caseIdList.add(testCase.getId());
                testCase.setCaseContent("");
            }
            List<RecordNum> recordNumList = execRecordMapper.getRecordNumByCaseIds(caseIdList);
            Map<Long, Integer> recordNumMap = Maps.newHashMap();
            if (recordNumList.size() > 0) {
                for (RecordNum recordNum : recordNumList) {
                    recordNumMap.put(recordNum.getCaseId(), recordNum.getRecordNum());
                }
            }
            for (TestCase testCase : testCaseList) {
                Long caseId = testCase.getId();
                Integer num = recordNumMap.get(caseId);
                if (num == null) {
                    testCase.setRecordNum(0);
                } else {
                    testCase.setRecordNum(num);
                }
            }
        }
        if (CollectionUtils.isEmpty(testCaseList)) {
            return new PageResult<>();
        }

        pageResult.setData(testCaseList);
        return pageResult;
    }

    private long countTestCase(Long productLineId, Integer case_type, Long id, String title, String creator, String[] requirement_id, Date beginTime, Date endTime, Integer channel) {

        return testCaseMapper.getCountTestCase(productLineId, case_type, id, title, creator, requirement_id, beginTime, endTime, channel);
    }

    @Override
    public TestCase clearTestCaseById(Long id) {
        String beforeContent = selectCaseById(String.valueOf(id));
        String pattern = "\\\"progress\\\"\\:\\d";
        String afterPattern = "\\\"progress\\\"\\:null";
        String afterContent = beforeContent.replaceAll(pattern, afterPattern);

        TestCase testCase = new TestCase();
        testCase.setCaseContent(afterContent);
        testCase.setId(id);
        update(testCase);
        return testCase;
    }

    @Override
    public TestCase clearTestCase(TestCase testCaseObj) {
        String beforeContent = testCaseObj.getCaseContent();
        String pattern = "\\\"progress\\\"\\:\\d";
        String afterPattern = "\\\"progress\\\"\\:null";
        String afterContent = beforeContent.replaceAll(pattern, afterPattern);
        testCaseObj.setCaseContent(afterContent);
        update(testCaseObj);
        return testCaseObj;
    }

    /**
     * 导入用例
     */
    @Override
    public TestCase getJsonByXmind(String file, String creator, Long productLine_id, String requirementId, String title, String description, Integer channel) {
        try {
            TestCase testCase = new TestCase();

            testCase.setTitle(title);
            testCase.setCaseType(0);
            testCase.setIsDelete(0);
            testCase.setCreator(creator);
            testCase.setProductLineId(productLine_id);
            testCase.setRequirementId(requirementId);
            testCase.setDescription(description);
            if (channel != null) {
                testCase.setChannel(channel);
            }

            IWorkbookBuilder builder = Core.getWorkbookBuilder();
            IWorkbook workbook = null;
            try {
                workbook = builder.loadFromPath(file);//打开XMind文件
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CoreException e) {
                e.printStackTrace();
            }
            ISheet defSheet = workbook.getPrimarySheet();//获取主Sheet
            ITopic rootTopic = defSheet.getRootTopic(); //获取根Topic

            String rootId = rootTopic.getId();
            //暂不支持xmind zen版本
            if (rootId.equals("1vr0lcte2og4t2sopiogvdmifc")) {
                log.error("import case failed,the version isnot match");
                testCase.setId(1L);
                return testCase;
            } else {
                JSONObject caseObj = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                TreeUtil.importData(jsonArray, rootTopic);

                caseObj.put("root", jsonArray.get(0));
                caseObj.put("template", "default");
                caseObj.put("theme", "fresh-blue");
                caseObj.put("version", "1.4.43");
                caseObj.put("base", "16");

//            NodeObj nodelist = insertJsonChild(rootTopic);
//            //获取root下data和child
//            NodeObj nodeList = insertJsonChild(rootTopic);

                testCase.setCaseContent(caseObj.toJSONString());

                createTestCase(testCase);
                log.info("import case success.  id:" + testCase.getId() + "\t title:" + testCase.getTitle());
                return testCase;
            }
        } catch (Exception e) {
            log.error("import failed. ", e);
            throw new ResponseException(ErrorCode.VERSION_NOT_MATCH, "用例导入失败 : " + e.getMessage());
        }
    }

    /**
     * 解析xmind的json文件导入用例
     */
    @Override
    public TestCase getJsonByXmindJson(String file, String creator, Long productLine_id, String requirementId, String title, String description, Integer channel) {
        try {
            TestCase testCase = new TestCase();

            testCase.setTitle(title);
            testCase.setCaseType(0);
            testCase.setIsDelete(0);
            testCase.setCreator(creator);
            testCase.setProductLineId(productLine_id);
            testCase.setRequirementId(requirementId);
            testCase.setDescription(description);
            if (channel != null) {
                testCase.setChannel(channel);
            }

            String s = FileUtil.readJsonFile(file);
            JSONArray parseArray = JSONObject.parseArray(s);
            JSONObject getObj = parseArray.getJSONObject(0);
            JSONObject rootTopic = getObj.getJSONObject("rootTopic");

            JSONObject caseObj = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            TreeUtil.importDataByJson(jsonArray, rootTopic);

            caseObj.put("root", jsonArray.get(0));
            caseObj.put("template", "default");
            caseObj.put("theme", "fresh-blue");
            caseObj.put("version", "1.4.43");
            caseObj.put("base", "16");

//            NodeObj nodelist = insertJsonChild(rootTopic);
//            //获取root下data和child
//            NodeObj nodeList = insertJsonChild(rootTopic);

            testCase.setCaseContent(caseObj.toJSONString());

            createTestCase(testCase);
            log.info("import case success.  id:" + testCase.getId() + "\t title:" + testCase.getTitle());
            return testCase;

        } catch (Exception e) {
            log.error("import failed. ", e);
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "用例导入失败 : " + e.getMessage());
        }
    }

    /**
     * 根据用例id，导出用例
     */
    @Override
    public HttpServletResponse getXmindById(Long id, HttpServletResponse response, HttpServletRequest request) {
        String file = "";
        File file1 = new File("");
        String filePath = "";
        try {
            filePath = file1.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TestCase testCase = null;
        IWorkbookBuilder builder = Core.getWorkbookBuilder();
        IWorkbook workbook = builder.createWorkbook();
        ISheet defSheet = workbook.getPrimarySheet();

        try {
            testCase = testCaseMapper.selectByPrimaryKey(id);
            String caseContent = testCase.getCaseContent();
            String title = testCase.getTitle();
            ITopic rootTopic = defSheet.getRootTopic();
            JSONObject rootObj = JSON.parseObject(caseContent).getJSONObject("root");
            rootTopic.setTitleText(rootObj.getJSONObject("data").getString("text"));
            rootTopic.setFolded(false);

            TreeUtil.exportData(rootObj.getJSONArray("children"), workbook, rootTopic);

            defSheet.setThemeId("xminddefaultthemeid2014");
            defSheet.setStyleId("xminddefaultthemeid2014");
            Long time = new Date().getTime();
            String fileName = title + "_" + time + "_u.xmind";
            workbook.save(fileName);
            file = filePath + "/" + fileName;

            String filename = "";
            //下载显示的文件名
            //兼容不同浏览器
            String userAgent = request.getHeader("User-Agent");
            if (userAgent.contains("Chrome") || userAgent.contains("Safari") || userAgent.contains("Firefox")) {
                log.info("Chrome/Safari/Firefox浏览器");
                filename = new String(fileName.getBytes(), "ISO-8859-1");
            } else {
                log.info("IE浏览器");
                filename = new String(fileName.getBytes(), "UTF-8");
            }

            //下载文件
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            response.setHeader("content-Type", "application/octet-stream");

            OutputStream outputStream = response.getOutputStream();
            InputStream inputStream = new FileInputStream(file);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = inputStream.read(buffer)) > 0) {
                //将缓冲区的数据输出到客户端浏览器
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            File file2 = new File(file);
            FileUtil.delete(file2);

        } catch (Exception e) {
            log.error("Failed to get testCaseObject ", e);
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "Failed to get testCaseObject : " + e.getMessage());
        }
        return response;
    }

    public List<Map<String, Object>> listCreators(Integer case_type, Long productLineId) {
        List<String> result = testCaseMapper.listCreators(case_type, productLineId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (String name : result) {
            Map<String, Object> value = new HashMap<>();
            value.put("id", 0);
            value.put("staffNamePY", name);
            value.put("staffNameCN", name);
            res.add(value);
        }

        res = res.stream().sorted(Comparator.comparing((Map<String, Object> h) -> ((String) h.get("staffNameCN")))).collect(Collectors.toList());
        return res;
    }

    @Override
    public TestCasePriorityInfoResp getPriorityInfoByCaseId(Long caseId) {
        TestCasePriorityInfoResp testCasePriorityInfoResp = new TestCasePriorityInfoResp();

        TestCase testCase = testCaseMapper.selectByPrimaryKey(caseId);

        List<Integer> levelCaseNum = new ArrayList<>();

        for (PriorityEnv level : PriorityEnv.values()) {
            JSONObject content = JSON.parseObject(testCase.getCaseContent());
            JSONObject caseRoot = content.getJSONObject("root");
            Stack<JSONObject> objCheck = new Stack<>();
            Stack<IntCount> iCheck = new Stack<>();
            objCheck.push(caseRoot);
            List<String> list = new ArrayList<>();
            list.add(level.getValue().toString());
            TreeUtil.getPriority(objCheck, iCheck, caseRoot, list);

            IntCount totalCount = new IntCount(0);
            if (caseRoot.getJSONArray("children").size() != 0) {
                TreeUtil.getCaseNum(caseRoot, totalCount);
            }

            levelCaseNum.add(totalCount.get());
        }

        int totalCount = TreeUtil.parse(testCase.getCaseContent()).getInteger("totalCount");

        testCasePriorityInfoResp.setLevelCaseNum(levelCaseNum);
        testCasePriorityInfoResp.setTotalCount(totalCount);

        return testCasePriorityInfoResp;
    }

    @Override
    public ConditionTestCaseInfoVO getCountByCondition(JSONObject jsonObject) {

        ConditionTestCaseInfoVO res = new ConditionTestCaseInfoVO();

        String caseId = jsonObject.getString("caseId");
        JSONArray priority = jsonObject.getJSONArray("priority");
        JSONArray resource = jsonObject.getJSONArray("resource");
        if (caseId == null || priority == null || resource == null)
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "参数异常");

        TestCase testCase = testCaseMapper.selectByPrimaryKey(Long.valueOf(caseId));
        if (testCase == null) throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "caseId 有错");
        JSONObject content = JSONObject.parseObject(testCase.getCaseContent());
        JSONObject caseRoot = content.getJSONObject("root");

        HashSet<String> tags = new HashSet<>();
        Integer caseNum = TreeUtil.getCaseNum(caseRoot, tags);

        res.setTotalCount(caseNum);
        res.setTaglist(tags);

        HashSet<String> prioritySet = new HashSet<>(), resourceSet = new HashSet<>();
        if (priority.size() != 0) {
            for (Object o : priority) prioritySet.add((String) o);
            if (!TreeUtil.getChosenCase(caseRoot, prioritySet, "priority")) caseRoot = null;
        }
        if (resource.size() != 0) {
            for (Object o : resource) resourceSet.add((String) o);
            if(!TreeUtil.getChosenCase(caseRoot, resourceSet, "resource")) caseRoot = null;
        }


        //没有筛选，返回caseNum为null
        caseNum = (priority.size() == 0 && resource.size() == 0) ?
                null : TreeUtil.getCaseNum(caseRoot, tags);

        res.setCount(caseNum);

        return res;


    }

    @Override
    public int caseAddRequirementId(TestCase testCase) {
        String org_requirement_Id = testCaseMapper.selectByPrimaryKey(testCase.getId()).getRequirementId();
        if (org_requirement_Id.contains(testCase.getRequirementId())) {
            return 1;
        }

        String requirement_Id = "";

        if (org_requirement_Id == null || org_requirement_Id.equals("") || org_requirement_Id.equals("0")) {
            requirement_Id = testCase.getRequirementId();
        } else {
            requirement_Id = org_requirement_Id + "," + testCase.getRequirementId();
        }

        int ret = -1;
        try {
            ret = testCaseMapper.updateRequirementId(testCase.getId(), requirement_Id);
        } catch (Exception e) {
            log.error("update exception " + e);
        }

        return ret;
    }
}

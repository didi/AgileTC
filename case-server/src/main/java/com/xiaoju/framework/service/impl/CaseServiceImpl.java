package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.DirNodeDto;
import com.xiaoju.framework.entity.dto.RecordNumDto;
import com.xiaoju.framework.entity.dto.RecordWsDto;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.persistent.Biz;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.request.cases.CaseConditionReq;
import com.xiaoju.framework.entity.request.cases.CaseCreateReq;
import com.xiaoju.framework.entity.request.cases.CaseEditReq;
import com.xiaoju.framework.entity.request.cases.CaseQueryReq;
import com.xiaoju.framework.entity.request.ws.WsSaveReq;
import com.xiaoju.framework.entity.response.PersonResp;
import com.xiaoju.framework.entity.response.cases.CaseConditionResp;
import com.xiaoju.framework.entity.response.cases.CaseDetailResp;
import com.xiaoju.framework.entity.response.cases.CaseGeneralInfoResp;
import com.xiaoju.framework.entity.response.cases.CaseListResp;
import com.xiaoju.framework.entity.response.controller.PageModule;
import com.xiaoju.framework.entity.response.dir.BizListResp;
import com.xiaoju.framework.entity.response.dir.DirTreeResp;
import com.xiaoju.framework.handler.Room;
import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.mapper.BizMapper;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.service.CaseService;
import com.xiaoju.framework.service.DirService;
import com.xiaoju.framework.service.RecordService;
import com.xiaoju.framework.util.BitBaseUtil;
import com.xiaoju.framework.util.TimeUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.xiaoju.framework.constants.SystemConstant.COMMA;
import static com.xiaoju.framework.constants.SystemConstant.IS_DELETE;

/**
 * 用例实现类
 *
 * @author didi
 * @date 2020/9/7
 */
@Service
public class CaseServiceImpl implements CaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseServiceImpl.class);

    @Resource
    private BizMapper bizMapper;

    @Resource
    private DirService dirService;

    @Resource
    private TestCaseMapper caseMapper;

    @Resource
    private ExecRecordMapper recordMapper;

    @Resource
    private RecordService recordService;

    @Resource
    private CaseBackupService caseBackupService;

    @Override
    public PageModule<CaseListResp> getCaseList(CaseQueryReq request) {
        List<CaseListResp> res = new ArrayList<>();
        List<Long> caseIds = dirService.getCaseIds(request.getLineId(), request.getBizId(), request.getChannel());

        if (CollectionUtils.isEmpty(caseIds)) {
            return PageModule.emptyPage();
        }

        Date beginTime = transferTime(request.getBeginTime());
        Date endTime = transferTime(request.getEndTime());
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        // select * from test_case where case_id in (request.getCaseIds()) [and ...any other condition];
        List<TestCase> caseList = caseMapper.search(request.getCaseType(), caseIds, request.getTitle(), request.getCreator(),
                request.getRequirementId(), beginTime, endTime, request.getChannel(), request.getLineId(), request.getCaseKeyWords());
        List<RecordNumDto> recordNumDtos = recordMapper.getRecordNumByCaseIds(caseIds);
        Map<Long, Integer> recordMap = recordNumDtos.stream().collect(Collectors.toMap(RecordNumDto::getCaseId, RecordNumDto::getRecordNum));

        for (TestCase testCase : caseList) {
            res.add(buildListResp(testCase, recordMap.get(testCase.getId())));
        }
        return PageModule.buildPage(res, ((Page<TestCase>) caseList).getTotal());
    }

    @Override
    public CaseDetailResp getCaseDetail(Long caseId) {
        TestCase testCase = caseMapper.selectOne(caseId);
        if (testCase == null) {
            throw new CaseServerException("用例不存在", StatusCode.INTERNAL_ERROR);
        }
        if (testCase.getIsDelete().equals(IS_DELETE)) {
            throw new CaseServerException("用例已删除", StatusCode.INTERNAL_ERROR);
        }
        return buildDetailResp(testCase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertOrDuplicateCase(CaseCreateReq request) {
        TestCase testcase = buildCase(request);
        caseMapper.insert(testcase);
        // 可能会多个加入  所以不要使用dirService.addCase()
        DirNodeDto tree = dirService.getDirTree(testcase.getProductLineId(), testcase.getChannel());
        List<String> addBizs = Arrays.asList(request.getBizId().split(SystemConstant.COMMA));
        updateDFS(packageTree(tree), String.valueOf(testcase.getId()), new HashSet<>(addBizs), new HashSet<>());
        updateBiz(testcase, tree);
        return testcase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DirTreeResp updateCase(CaseEditReq request) {
        TestCase testCase = caseMapper.selectOne(request.getId());
        if (testCase == null) {
            throw new CaseServerException("用例不存在", StatusCode.NOT_FOUND_ENTITY);
        }

        List<String> addBizs = getDiffSet(request.getBizId(), testCase.getBizId());
        List<String> rmBizs = getDiffSet(testCase.getBizId(), request.getBizId());

        BeanUtils.copyProperties(request, testCase);
        testCase.setGmtModified(new Date());

        DirNodeDto tree = dirService.getDirTree(testCase.getProductLineId(), testCase.getChannel());
        updateDFS(packageTree(tree), String.valueOf(request.getId()), new HashSet<>(addBizs), new HashSet<>(rmBizs));
        updateBiz(testCase, tree);

        caseMapper.update(testCase);

        return dirService.getAllCaseDir(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DirTreeResp deleteCase(Long caseId) {
        TestCase testCase = caseMapper.selectOne(caseId);
        testCase.setIsDelete(IS_DELETE);

        // 删除所有操作记录
        List<ExecRecord> execRecords = recordMapper.getRecordListByCaseId(testCase.getId());
        if (!CollectionUtils.isEmpty(execRecords)) {
            recordMapper.batchDelete(execRecords.stream().map(ExecRecord::getId).collect(Collectors.toList()));
        }

        DirNodeDto tree = dirService.getDirTree(testCase.getProductLineId(), testCase.getChannel());
        updateDFS(packageTree(tree), String.valueOf(caseId), new HashSet<>(), new HashSet<>(convertToList(testCase.getBizId())));
        updateBiz(testCase, tree);

        caseMapper.delete(testCase.getId());
        return dirService.getAllCaseDir(tree);
    }

    @Override
    public List<PersonResp> listCreators(Integer caseType, Long lineId) {
        List<PersonResp> list = new ArrayList<>();
        List<String> names = caseMapper.listCreators(caseType, lineId);

        if (CollectionUtils.isEmpty(names)) {
            return list;
        }

        return names.stream().map(name -> {
                    PersonResp person = new PersonResp();
                    person.setStaffNamePY(name);
                    // 这里目前是扔出去了英文名，有需要可以自己加
                    person.setStaffNameCN(name);
                    return person;
                }).collect(Collectors.toList());
    }


    @Override
    public CaseConditionResp getCountByCondition(CaseConditionReq req) {
        CaseConditionResp res = new CaseConditionResp();

        TestCase testCase = caseMapper.selectOne(req.getCaseId());
        JSONObject content = JSONObject.parseObject(testCase.getCaseContent());
        JSONObject caseRoot = content.getJSONObject("root");

        HashSet<String> tags = new HashSet<>();
        Integer caseNum = TreeUtil.getCaseNum(caseRoot, tags);

        res.setTotalCount(caseNum);
        res.setTaglist(tags);

        HashSet<String> prioritySet, resourceSet;
        if (!CollectionUtils.isEmpty(req.getPriority())) {
            prioritySet = new HashSet<>(req.getPriority());
            if (!TreeUtil.getChosenCase(caseRoot, prioritySet, "priority")) {
                caseRoot = null;
            }
        }
        if (!CollectionUtils.isEmpty(req.getResource())) {
            resourceSet = new HashSet<>(req.getResource());
            if (!TreeUtil.getChosenCase(caseRoot, resourceSet, "resource")) {
                caseRoot = null;
            }
        }
        //没有筛选，返回caseNum为null
        caseNum = (req.getPriority().size() == 0 && req.getResource().size() == 0) ? null : TreeUtil.getCaseNum(caseRoot, tags);
        res.setCount(caseNum);
        return res;
    }

    @Override
    public CaseGeneralInfoResp getCaseGeneralInfo(Long caseId) {
        TestCase testCase = caseMapper.selectOne(caseId);
        if (testCase == null) {
            throw new CaseServerException("用例不存在", StatusCode.NOT_FOUND_ENTITY);
        }
        CaseGeneralInfoResp resp = new CaseGeneralInfoResp();
        resp.setId(testCase.getId());
        resp.setProductLineId(testCase.getProductLineId());
        resp.setRequirementId(testCase.getRequirementId());
        resp.setTitle(testCase.getTitle());
        return resp;
    }

    @Override
    public void wsSave(WsSaveReq req) {
//        List<String> editors = WebSocket.getEditingUser(String.valueOf(req.getId()),
//                StringUtils.isEmpty(req.getRecordId())?"undefined":String.valueOf(req.getRecordId()));
//        if (editors.size() < 1) {
//            throw new CaseServerException("用例ws链接已经断开，当前保存可能丢失，请刷新页面重建ws链接。", StatusCode.WS_UNKNOWN_ERROR);
//        }

        LOGGER.info(Thread.currentThread().getName() + ": http开始保存用例。");

        CaseBackup caseBackup = new CaseBackup();
        // 这里触发保存record
        if (!StringUtils.isEmpty(req.getRecordId())) {
            synchronized (WebSocket.getRoomLock()) {
                RecordWsDto dto = recordService.getWsRecord(req.getRecordId());
                // 看看是不是有重合的执行人
                List<String> names = Arrays.stream(dto.getExecutors().split(COMMA)).filter(e -> !StringUtils.isEmpty(e)).collect(Collectors.toList());
                long count = names.stream().filter(e -> e.equals(req.getModifier())).count();
                String executors;
                if (count > 0) {
                    // 有重合，不管了
                    executors = dto.getExecutors();
                } else {
                    // 没重合往后面塞一个
                    names.add(req.getModifier());
                    executors = String.join(",", names);
                }

                JSONObject jsonObject = TreeUtil.parse(req.getCaseContent());
                ExecRecord record = new ExecRecord();
                record.setId(req.getRecordId());
                record.setCaseId(req.getId());
                record.setModifier(req.getModifier());
                record.setGmtModified(new Date(System.currentTimeMillis()));
                record.setCaseContent(jsonObject.getJSONObject("progress").toJSONString());
                record.setFailCount(jsonObject.getInteger("failCount"));
                record.setBlockCount(jsonObject.getInteger("blockCount"));
                record.setIgnoreCount(jsonObject.getInteger("ignoreCount"));
                record.setPassCount(jsonObject.getInteger("passCount"));
                record.setTotalCount(jsonObject.getInteger("totalCount"));
                record.setSuccessCount(jsonObject.getInteger("successCount"));
                record.setExecutors(executors);
                if (null == WebSocket.getRoom(false, BitBaseUtil.mergeLong(req.getRecordId(), Long.valueOf(req.getId())))) {
                    recordService.modifyRecord(record);
                    LOGGER.info("当前是最后一个退出, 落库保存执行记录.");
                } else {
                    LOGGER.info("还有编辑人未退出, 不落库保存执行记录.");
                }
            }
            caseBackup.setCaseId(req.getRecordId());
            caseBackup.setRecordContent(req.getCaseContent());
            caseBackup.setCaseContent("");
        } else {
            // 这里触发保存testcase
            synchronized (WebSocket.getRoomLock()) {
                TestCase testCase = caseMapper.selectOne(req.getId());
//                String caseContent = testCase.getCaseContent();
                JSONObject caseContentToSave = JSON.parseObject(req.getCaseContent());
                JSONObject caseContentBase = JSON.parseObject(testCase.getCaseContent());
//                if (caseContentToSave.getInteger("base") > caseContentBase.getInteger("base")) {
//                    // 保存落库
//
//                } else {
                    // 保存落库,且发送消息给其他客户端(如果有)
                ObjectMapper jsonMapper = new ObjectMapper();
                Room room = WebSocket.getRoom(false, BitBaseUtil.mergeLong(0l, Long.valueOf(req.getId())));
                if (null != room && room.getTestCaseContent() != null) {
                    try {
                        JsonNode baseContent = jsonMapper.readTree(room.getTestCaseContent());
                        JsonNode reqContent = jsonMapper.readTree(req.getCaseContent());

                        ArrayNode patches = (ArrayNode) JsonDiff.asJson(baseContent, reqContent);
                        if (patches.size() > 1) { // 此处待验证
                            JsonNodeFactory FACTORY = JsonNodeFactory.instance;
                            ObjectNode basePatch = FACTORY.objectNode();
                            basePatch.put("op", "replace");
                            basePatch.put("path", "/base");
                            basePatch.put("value", reqContent.get("base").asLong() + 1);
                            patches.add(basePatch);
                            room.leavebroadcastMessageForHttp(req.getCaseContent());
                            room.setTestCaseContent(req.getCaseContent());
                            LOGGER.info("非最后离开, 将变更补丁信息发送给其他用户. req内容是: ", reqContent.toString());
                            LOGGER.info("room内容是: ", baseContent.toString());
                        } else {
                            LOGGER.info("内存中数据与待保存数据一致." + patches.toString());
                        }
                    } catch (Exception e) {
                        LOGGER.error("http保存比较内容失败.", e);
                    }
                } else {
                    LOGGER.info("websocket实例已经退出或未编辑,无需发送消息.");
                }
//                }

                testCase.setCaseContent(req.getCaseContent());
                testCase.setGmtModified(new Date(System.currentTimeMillis()));
                int ret = caseMapper.update(testCase);
                if (ret < 1) {
                    LOGGER.error(Thread.currentThread().getName() + ": 数据库用例内容更新失败。http save ret = " + ret);
                    LOGGER.error("http应该保存的用例内容是：" + req.getCaseContent());
                }

            }
            caseBackup.setCaseId(req.getId());
            caseBackup.setCaseContent(req.getCaseContent());
            caseBackup.setRecordContent("");
        }
        caseBackup.setCreator(req.getModifier());
        caseBackup.setExtra("");
        caseBackupService.insertBackup(caseBackup);

        LOGGER.info(Thread.currentThread().getName() + ": http开始保存结束。");

    }

    /**
     * 字符串时间转date
     *
     * @param time 时间字符串
     * @return 如果字符串为空，那么Date也为空
     */
    private Date transferTime(String time) {
        if (time == null) {
            return null;
        }
        return TimeUtil.transferStrToDateInSecond(time);
    }

    private List<String> getDiffSet(String newStr, String oldStr) {
        List<String> newIds = convertToList(newStr);
        List<String> oldIds = convertToList(oldStr);
        newIds.removeIf(oldIds::contains);
        return newIds;
    }

    private List<String> convertToList(String str) {
        return Arrays.stream(str.split(SystemConstant.COMMA)).collect(Collectors.toList());
    }

    /**
     * 构造/list下的用例列表
     *
     * @param testCase 测试用例
     * @return 列表单条
     * @see #getCaseList
     */
    private CaseListResp buildListResp(TestCase testCase, Integer recordNum) {
        CaseListResp resp = new CaseListResp();
        BeanUtils.copyProperties(testCase, resp);
        resp.setRecordNum(recordNum == null ? 0 : recordNum);
        return resp;
    }

    /**
     * 构造用例详情内容
     *
     * @param testCase 测试用例
     * @return 详情单条
     * @see #getCaseDetail
     */
    private CaseDetailResp buildDetailResp(TestCase testCase) {
        CaseDetailResp resp = new CaseDetailResp();
        BeanUtils.copyProperties(testCase, resp);
        resp.setBiz(
                getBizFlatList(testCase.getProductLineId(), Arrays.asList(testCase.getBizId().split(SystemConstant.COMMA)), testCase.getChannel())
                        .stream().filter(BizListResp::isSelect).collect(Collectors.toList())
        );
        resp.setProductLineId(testCase.getProductLineId());
        return resp;
    }

    /**
     * 查看详情时，显示关联的需求，以及所有的需求
     *
     * @param lineId 业务线id
     * @param bizIds 关联的文件夹id列表
     * @return 去掉顶级文件夹的文件夹树
     * @see #buildDetailResp
     */
    private List<BizListResp> getBizFlatList(Long lineId, List<String> bizIds, Integer channel) {
        DirNodeDto root = dirService.getDirTree(lineId, channel);
        List<BizListResp> list = new ArrayList<>();
        flatDfs(root, list, new ArrayList<>(), bizIds);
        // 一开始的root不要给出去
        list.remove(0);
        return list;
    }

    private void flatDfs(DirNodeDto node, List<BizListResp> list, List<String> path, List<String> bizIds) {
        list.add(buildBizList(node, path, bizIds));

        if (CollectionUtils.isEmpty(node.getChildren())) {
            return ;
        }

        for (int i = 0; i < node.getChildren().size(); i++) {
            path.add(node.getChildren().get(i).getText());
            flatDfs(node.getChildren().get(i), list, path, bizIds);
            path.remove(path.size() - 1);
        }
    }

    private BizListResp buildBizList(DirNodeDto node, List<String> path, List<String> bizIds) {
        BizListResp obj = new BizListResp();
        obj.setBizId(node.getId());
        obj.setText(String.join(">", path));
        obj.setSelect(bizIds.contains(node.getId()));
        return obj;
    }

    /**
     * 新建/复制时，构建新的用例
     *
     * @param request 请求体
     * @return 新的请求体
     * @see #insertOrDuplicateCase
     */
    private TestCase buildCase(CaseCreateReq request) {
        String content = request.getCaseContent();
        // 如果是复制
        if (request.getId() != null) {
            TestCase testCase = caseMapper.selectOne(request.getId());
            if (testCase == null) {
                throw new CaseServerException("用例不存在", StatusCode.NOT_FOUND_ENTITY);
            }
            content = testCase.getCaseContent();
        }

        TestCase ret = new TestCase();
        ret.setTitle(request.getTitle());
        ret.setRequirementId(request.getRequirementId());
        ret.setBizId(request.getBizId());
        ret.setGroupId(1L);
        ret.setProductLineId(request.getProductLineId());
        ret.setDescription(request.getDescription());
        ret.setCreator(request.getCreator());
        ret.setModifier(ret.getCreator());
        ret.setChannel(request.getChannel());
        ret.setExtra(SystemConstant.EMPTY_STR);
        ret.setGmtCreated(new Date());
        ret.setGmtModified(new Date());
        ret.setCaseContent(content);
        return ret;
    }

    /**
     * 更新json体
     *
     * @param node 树
     * @param addSet 需要新增caseId的set
     * @param rmSet 需要删减caseId的set
     */
    private void updateDFS(DirNodeDto node, String caseId, Set<String> addSet, Set<String> rmSet) {
        if (CollectionUtils.isEmpty(node.getChildren())) {
            return ;
        }

        for (int i = 0; i < node.getChildren().size(); i++) {
            DirNodeDto childNode = node.getChildren().get(i);
            if (addSet.contains(childNode.getId())) {
                childNode.getCaseIds().add(caseId);
            }
            if (rmSet.contains(childNode.getId())) {
                childNode.getCaseIds().remove(caseId);
            }
            updateDFS(childNode, caseId, addSet, rmSet);
        }
    }

    /**
     * dir-封装一下树的开头，这样树的头结点也可以进行插入
     */
    private DirNodeDto packageTree(DirNodeDto node) {
        DirNodeDto pack = new DirNodeDto();
        pack.getChildren().add(node);
        return pack;
    }

    /**
     * 更新文件夹
     *
     * @param testCase 测试用例
     * @param tree 树
     */
    public void updateBiz(TestCase testCase, DirNodeDto tree) {
        Biz biz = bizMapper.selectOne(testCase.getProductLineId(), testCase.getChannel());
        biz.setContent(JSON.toJSONString(tree));
        biz.setGmtModified(new Date());
        bizMapper.update(biz);
    }
}

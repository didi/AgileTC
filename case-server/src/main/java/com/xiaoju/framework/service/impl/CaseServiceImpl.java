package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.ApplyPatchResultDto;
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
import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.mapper.BizMapper;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.service.CaseService;
import com.xiaoju.framework.service.DirService;
import com.xiaoju.framework.service.RecordService;
import com.xiaoju.framework.util.MinderJsonPatchUtil;
import com.xiaoju.framework.util.TimeUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.xiaoju.framework.constants.SystemConstant.COMMA;
import static com.xiaoju.framework.constants.SystemConstant.IS_DELETE;
import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_EXPAND_STATE_CONFLICT;
import static com.xiaoju.framework.constants.enums.ApplyPatchFlagEnum.IGNORE_REPLACE_ORDER_CONFLICT;
import static com.xiaoju.framework.util.MinderJsonPatchUtil.cleanAllBackground;

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
        List<TestCase> caseList = caseMapper.search(request.getCaseType(), caseIds, request.getTitle(),
                request.getCreator(), request.getRequirementId(), beginTime, endTime, request.getChannel(), request.getLineId());

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
    public String wsSave(WsSaveReq req) {
        // TODO: 这个方法逻辑有点太复杂了，需要再拆开一些 private 的子方法
        String recordInfo = "";
        String conflictMessage = "";
        String saveExecRecordMessage = "";
        List<String> applyPatch = new ArrayList<>();
        String returnMessage = "";

        // 这里触发保存record
        if (!ObjectUtils.isEmpty(req.getRecordId())) {
            RecordWsDto dto = recordService.getWsRecord(req.getRecordId());
            saveRecord(req, dto);

            ExecRecord record = recordMapper.selectOne(req.getRecordId());
            // 简单记录下信息，后续 backup 记录用到
            recordInfo = "|任务名称:" + record.getTitle() + ",任务id:" + record.getId();

            saveExecRecordMessage = "用例执行结果保存成功。";

            if (!StringUtils.isEmpty(req.getBaseCaseContent())) {
                // 后续要进行用例的增量保存，所以把 baseCaseContent 和 caseContent 里面的 progress 都去掉
                req.setBaseCaseContent(MinderJsonPatchUtil.cleanAllProgress(req.getBaseCaseContent()));
                req.setCaseContent(MinderJsonPatchUtil.cleanAllProgress(req.getCaseContent()));
            }
        }
        // 统一保存用例的变更
        TestCase testCase = caseMapper.selectOne(req.getId());

        if (testCase.getCaseContent().equals(req.getCaseContent())) {
            return saveExecRecordMessage + "检测到现有内容和数据库一致，无需保存";
        }


        if (StringUtils.isEmpty(req.getBaseCaseContent())) {
            if (ObjectUtils.isEmpty(req.getRecordId())) {
                // 老的模式，直接全量保存，和旧逻辑保持一致。仅作为服务端已更新，前端未更新期间避免无法保存时用
                LOGGER.warn("检测到仍在使用直接覆盖的方式进行用例保存。保存者：{}", req.getModifier());
                testCase.setCaseContent(req.getCaseContent());
                // 保存到数据库的用例中，不应该带有任何 background 信息，避免影响预览时标记变更内容
                testCase.setCaseContent(cleanAllBackground(testCase.getCaseContent()));
                testCase.setModifier(req.getModifier());
                caseMapper.update(testCase);

                // 保存成功，也存一个新的备份
                CaseBackup caseBackup = new CaseBackup();
                caseBackup.setCaseId(testCase.getId());
                caseBackup.setTitle(req.getSaveReason() + recordInfo);
                caseBackup.setCreator(testCase.getModifier());
                caseBackup.setGmtCreated(new Date());
                caseBackup.setCaseContent(testCase.getCaseContent());
                caseBackupService.insertBackup(caseBackup);

                returnMessage = saveExecRecordMessage + "用例集改动全量保存成功";
            }

        } else {
            // 前端可返回 base 版本 json 和改动后版本 json 时，使用增量保存
            try {
                String allPatch = MinderJsonPatchUtil.getContentPatch(req.getBaseCaseContent(), req.getCaseContent());
                LOGGER.info("需要应用的 patch: {}", allPatch);

                if (JSONArray.parseArray(allPatch).isEmpty()) {
                    return saveExecRecordMessage + "检测到本次用例内容没有内容变更，无需保存";
                }

                // 忽略调整 order 相关冲突及展开状态修改相关冲突
                ApplyPatchResultDto applyPatchResultDto = MinderJsonPatchUtil.batchApplyPatch(allPatch,
                        testCase.getCaseContent(), EnumSet.of(IGNORE_REPLACE_ORDER_CONFLICT, IGNORE_EXPAND_STATE_CONFLICT));
                String caseContentAfterPatch = applyPatchResultDto.getJsonAfterPatch();
                List<String> conflictPatches = applyPatchResultDto.getConflictPatch();
                applyPatch = applyPatchResultDto.getApplyPatch();

                // 能应用上的要保存一下
                testCase.setCaseContent(caseContentAfterPatch);

                if (!conflictPatches.isEmpty()) {
                    LOGGER.warn("在用例id {} 上应用增量保存，发现部分改动无法应用。无法应用的改动为：{}", req.getId(),
                            JSONObject.toJSONString(conflictPatches));
                    // 存在冲突，先单独保存一次备份，避免数据丢失
                    CaseBackup conflictCaseBackup = new CaseBackup();
                    conflictCaseBackup.setCaseId(req.getId());
                    conflictCaseBackup.setTitle("合并冲突自动保存副本" + recordInfo);
                    conflictCaseBackup.setCreator(req.getModifier());
                    conflictCaseBackup.setGmtCreated(new Date());
                    conflictCaseBackup.setCaseContent(req.getCaseContent());
                    // 备份保存，也保存一下冲突的 patch 内容，便于后面生成相关指示信息
                    conflictCaseBackup.setIsConflict(true);
                    conflictCaseBackup.setJsonPatch(convertPatchListToJsonArray(conflictPatches));

                    CaseBackup dbCaseBackup = caseBackupService.insertBackup(conflictCaseBackup);

                    // 请勿改动，前端根据这个来识别本次改动结果信息
                    String backupMsg = "backupId=" + dbCaseBackup.getId();
                    conflictMessage = saveExecRecordMessage +  "用例改动增量保存失败，部分内容修改内容和数据库最新版本存在冲突，请手动处理。" + backupMsg;
                }

                // 看有没有应用成功变更的，有的话更新下数据库内容并存备份，没有的话不用保存
                if (!applyPatch.isEmpty()) {
                    // 保存到数据库的用例中，不应该带有任何 background 信息，避免影响预览时标记变更内容
                    testCase.setCaseContent(cleanAllBackground(testCase.getCaseContent()));
                    testCase.setModifier(req.getModifier());
                    caseMapper.update(testCase);

                    // 保存成功，也存一个新的备份
                    CaseBackup caseBackup = new CaseBackup();
                    caseBackup.setCaseId(testCase.getId());
                    caseBackup.setTitle(req.getSaveReason() + recordInfo);
                    caseBackup.setCreator(testCase.getModifier());
                    caseBackup.setGmtCreated(new Date());
                    caseBackup.setCaseContent(testCase.getCaseContent());
                    // 备份保存，也保存一下本次 patch 的内容，便于看每次改动内容时进行识别
                    caseBackup.setIsConflict(false);
                    caseBackup.setJsonPatch(convertPatchListToJsonArray(applyPatch));
                    caseBackupService.insertBackup(caseBackup);
                }

                // 如果前面有冲突，需要返回 exception
                if (!StringUtils.isEmpty(conflictMessage)) {
                    throw new CaseServerException(conflictMessage, StatusCode.SAVE_CONFLICT);
                }

                returnMessage = saveExecRecordMessage + "用例集改动保存成功";
            } catch (IOException e) {
                throw new CaseServerException("解析需增量保存的 json 失败，请确认提交的信息格式正确", e, StatusCode.DATA_FORMAT_ERROR);
            }
        }

        return returnMessage;

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

    private String convertPatchListToJsonArray(List<String> patchList) {
        JSONArray patchJsonArray = new JSONArray();

        for (String patch : patchList) {
            // 也校验一下是否为有效的 json 格式。如果不是，直接报错吧。
            patchJsonArray.add(JSONObject.parse(patch));
        }

        return patchJsonArray.toJSONString();
    }

    // 原来保存测试记录的逻辑
    private void saveRecord(WsSaveReq req, RecordWsDto dto) {
        // 看看是不是有重合的执行人
        List<String> names = Arrays.stream(dto.getExecutors().split(COMMA)).filter(e->!StringUtils.isEmpty(e)).collect(Collectors.toList());
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
        recordService.modifyRecord(record);
    }
}

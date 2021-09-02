package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.enums.EnvEnum;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.MergeCaseDto;
import com.xiaoju.framework.entity.dto.PickCaseDto;
import com.xiaoju.framework.entity.dto.RecordWsDto;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.request.record.RecordAddReq;
import com.xiaoju.framework.entity.request.record.RecordUpdateReq;
import com.xiaoju.framework.entity.request.ws.RecordWsClearReq;
import com.xiaoju.framework.entity.response.records.RecordGeneralInfoResp;
import com.xiaoju.framework.entity.response.records.RecordListResp;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.handler.Room;
import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.RecordService;
import com.xiaoju.framework.util.BitBaseUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.xiaoju.framework.constants.SystemConstant.EMPTY_STR;
import static com.xiaoju.framework.constants.SystemConstant.NOT_DELETE;
import static com.xiaoju.framework.util.TimeUtil.compareToOriginalDate;

/**
 * 执行任务实现类
 *
 * @author didi
 * @date 2020/9/23
 */
@Service
public class RecordServiceImpl implements RecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordServiceImpl.class);

    private static final String OE_PICK_ALL = "\"priority\":[\"0\"]";

    private static final Integer DEFAULT_ENV = 0;

    @Resource
    private ExecRecordMapper recordMapper;

    @Resource
    private TestCaseMapper caseMapper;

    @Override
    public List<RecordListResp> getListByCaseId(Long caseId) {
        List<RecordListResp> res = new ArrayList<>();
        TestCase testCase = caseMapper.selectOne(caseId);
        if (testCase == null) {
            throw new CaseServerException("用例不存在", StatusCode.NOT_FOUND_ENTITY);
        }

        List<ExecRecord> execRecordList = recordMapper.getRecordListByCaseId(caseId);
        for (ExecRecord record : execRecordList) {
            res.add(buildList(record));
        }
        return res;
    }

    @Override
    public RecordGeneralInfoResp getGeneralInfo(Long recordId) {
        ExecRecord record = recordMapper.selectOne(recordId);
        if (record == null) {
            throw new CaseServerException("操作记录不存在", StatusCode.NOT_FOUND_ENTITY);
        }

        TestCase testCase = caseMapper.selectOne(record.getCaseId());
        JSONObject merged = getData(new MergeCaseDto(testCase.getId(), record.getChooseContent(), record.getCaseContent(), record.getEnv(), 0L));

        // 开始构建响应体
        return buildGeneralInfoResp(record, testCase, merged);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRecord(RecordAddReq req) {
        // 根据caseContent和recordContent进行一次节点修剪合并
        JSONObject merged = getData(new MergeCaseDto(req.getCaseId(), req.getChooseContent(), EMPTY_STR, DEFAULT_ENV, 0L));
        ExecRecord record = buildExecRecord(req, merged);
        return recordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long recordId) {
        recordMapper.delete(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void editRecord(RecordUpdateReq req) {
        // 需要注意的是 圈选用例的对content的修改与脑图patch的修改不是同一频段
        // 所以这里修改的是圈选用例的话 一定要将websocket的redis清空
        ExecRecord record = recordMapper.selectOne(req.getId());
        if (record == null) {
            throw new CaseServerException("对应执行任务不存在", StatusCode.NOT_FOUND_ENTITY);
        }
        // 如果修改圈选用例时，有人在协同修改任务或者用例，那么就不让改
        if (!record.getChooseContent().equals(req.getChooseContent())) {
            Room room = getWsEditingCount(record);
            if (room != null && room.players.size() > 0) {
                LOGGER.info("[异步编辑任务属性]入参={}, 这些人正在编辑={}", req.toString(), room.getRoomPlayersName());
                throw new CaseServerException("当前" + room.getRoomPlayersName() + "正在修改，不允许修改圈选用例", StatusCode.INTERNAL_ERROR);
            }
        }
        // 日期类转换,在request.validate()已经解决单边输入为null的不完整问题
        if (req.getExpectStartTime() != null) {
            // 就是说这里是有日期区间的
            record.setExpectStartTime(new Date(req.getExpectStartTime()/1000*1000));
            record.setExpectEndTime(new Date(req.getExpectEndTime()/1000*1000));
        } else {
            // 没有区间设置默认值 1970-01-01 00:00:00
            record.setExpectStartTime(new Date(31507200000L));
            record.setExpectEndTime(new Date(31507200000L));
        }

        record.setModifier(req.getModifier());
        record.setTitle(req.getTitle());
        record.setChooseContent(req.getChooseContent());
        record.setDescription(StringUtils.isEmpty(req.getDescription()) ? EMPTY_STR : req.getDescription());
        record.setOwner(StringUtils.isEmpty(req.getOwner()) ? EMPTY_STR : req.getOwner());

        recordMapper.edit(record);
    }

    @Override
    public RecordWsDto getWsRecord(Long recordId) {
        ExecRecord record = recordMapper.selectOne(recordId);
        if (record == null) {
            throw new CaseServerException("执行任务不存在", StatusCode.NOT_FOUND_ENTITY);
        }
        RecordWsDto dto = new RecordWsDto();
        dto.setCaseContent(record.getCaseContent());
        dto.setChooseContent(record.getChooseContent());
        dto.setEnv(record.getEnv());
        dto.setExecutors(record.getExecutors());
        dto.setUpdateTime(record.getGmtModified());

        return dto;
    }

    @Override
    public void modifyRecord(ExecRecord record) {
        if (record == null) {
            throw new CaseServerException("对应执行任务不存在", StatusCode.NOT_FOUND_ENTITY);
        }
        if (StringUtils.isEmpty(record.getCaseContent())) {
            throw new CaseServerException("用例/任务内容为空", StatusCode.INTERNAL_ERROR);
        }
        if (StringUtils.isEmpty(record.getModifier())) {
            throw new CaseServerException("修改人为空", StatusCode.INTERNAL_ERROR);
        }
        ExecRecord dbRecord = recordMapper.selectOne(record.getId());
        BeanUtils.copyProperties(record, dbRecord);
        recordMapper.update(dbRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecRecord wsClearRecord(RecordWsClearReq req) {
        ExecRecord record = recordMapper.selectOne(req.getId());
        record.setCaseContent(EMPTY_STR);
        record.setSuccessCount(0);
        record.setPassCount(0);
        record.setFailCount(0);
        record.setBlockCount(0);
        record.setIgnoreCount(0);
        record.setModifier(req.getModifier());
        record.setGmtModified(new Date());

        recordMapper.update(record);

        JSONObject merged = getData(new MergeCaseDto(record.getCaseId(), record.getChooseContent(), record.getCaseContent(), record.getEnv(), record.getId()));
        record.setCaseContent(merged.get("content").toString());
        return record;
    }

    /**
     * 任务列表
     *
     * @param record 执行任务实体
     * @return 响应体
     */
    private RecordListResp buildList(ExecRecord record) {
        RecordListResp resp = new RecordListResp();
        resp.setId(record.getId());
        resp.setRecordId(record.getId());
        resp.setCaseId(record.getCaseId());
        resp.setTitle(record.getTitle());
        resp.setOwner(record.getOwner());
        resp.setCreator(record.getCreator());
        resp.setExecutors(record.getExecutors());

        // 其实本质上不能通过数据库去获取，因为还需要考虑chooseContent
        JSONObject object = getData(new MergeCaseDto(record.getCaseId(), record.getChooseContent(), record.getCaseContent(), record.getEnv(), 0L));
        resp.setBugNum(object.getInteger("failCount"));
        resp.setBlockNum(object.getInteger("blockCount"));
        resp.setSuccessNum(object.getInteger("successCount"));
        resp.setExecuteNum(object.getInteger("passCount"));
        resp.setTotalNum(object.getInteger("totalCount"));
        resp.setChooseContent(record.getChooseContent());
        resp.setCreateTime(record.getGmtCreated().getTime());
        resp.setDescription(record.getDescription());
        resp.setExpectStartTime(
                compareToOriginalDate(record.getExpectStartTime()) ? null : record.getExpectStartTime());
        resp.setExpectEndTime(
                compareToOriginalDate(record.getExpectEndTime()) ? null : record.getExpectEndTime());
        return resp;
    }

    /**
     * 构建任务实体
     *
     * @param req 请求体
     * @param merged 合并后的内容
     * @return
     */
    private ExecRecord buildExecRecord(RecordAddReq req, JSONObject merged) {
        ExecRecord record = new ExecRecord();
        // 统计信息传入
        record.setFailCount(merged.getInteger("failCount"));
        record.setBlockCount(merged.getInteger("blockCount"));
        record.setSuccessCount(merged.getInteger("successCount"));
        record.setPassCount(merged.getInteger("passCount"));
        record.setIgnoreCount(merged.getInteger("ignoreCount"));
        record.setTotalCount(merged.getInteger("totalCount"));
        // 基础信息传入
        record.setTitle(req.getTitle());
        record.setCaseId(req.getCaseId());
        record.setIsDelete(NOT_DELETE);
        record.setCaseContent(EMPTY_STR);
        record.setCreator(req.getCreator());
        record.setModifier(EMPTY_STR);
        record.setGmtCreated(new Date());
        record.setGmtModified(new Date());
        record.setChooseContent(StringUtils.isEmpty(req.getChooseContent()) ? EMPTY_STR : req.getChooseContent());
        record.setDescription(StringUtils.isEmpty(req.getDescription()) ? EMPTY_STR : req.getDescription());
        record.setExecutors(EMPTY_STR);
        record.setOwner(req.getOwner());
        record.setEnv(0);

        if (req.getExpectStartTime() != null) {
            // 就是说这里是有日期区间的
            record.setExpectStartTime(new Date(req.getExpectStartTime()/1000*1000));
            record.setExpectEndTime(new Date(req.getExpectEndTime()/1000*1000));
        } else {
            // 没有区间设置默认值
            record.setExpectStartTime(new Date(31507200000L));
            record.setExpectEndTime(new Date(31507200000L));
        }
        return record;
    }

    /**
     * 脑图统计条
     *
     * @param record 执行任务实体
     * @param testCase 用例
     * @param merged 合并后的json体
     * @return 响应体
     */
    private RecordGeneralInfoResp buildGeneralInfoResp(ExecRecord record, TestCase testCase, JSONObject merged) {
        RecordGeneralInfoResp resp = new RecordGeneralInfoResp();
        resp.setId(record.getId());
        resp.setTitle(record.getTitle());
        resp.setCaseId(testCase.getId());
        resp.setRequirementIds(testCase.getRequirementId());
        resp.setExpectStartTime(
                compareToOriginalDate(record.getExpectStartTime()) ? null : record.getExpectStartTime());
        resp.setExpectEndTime(
                compareToOriginalDate(record.getExpectEndTime()) ? null : record.getExpectEndTime());
        if (merged.getInteger("totalCount") == 0 || merged.getInteger("totalCount") == null) {
            // RecordGeneralInfoResp 内部给统计数据默认了0
            return resp;
        }
        resp.setPassCount(merged.getInteger("passCount"));
        resp.setBugNum(merged.getInteger("failCount"));
        resp.setSuccessCount(merged.getInteger("successCount"));
        resp.setBlockCount(merged.getInteger("blockCount"));
        resp.setIgnoreCount(merged.getInteger("ignoreCount"));
        resp.setTotalCount(merged.getInteger("totalCount"));

        BigDecimal passRate = BigDecimal.valueOf((double) resp.getSuccessCount() * 100 / (double) resp.getTotalCount());
        //通过率=执行成功用例/总用例
        resp.setPassRate(passRate.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        return resp;
    }

    /**
     * 获取一份用例下正在编辑的所有人  包括任务+用例
     *
     * @param record 执行任务实体
     * @return 响应体
     */
    public Room getWsEditingCount(ExecRecord record) {
        TestCase testCase = caseMapper.selectOne(record.getCaseId());
        if (testCase == null) {
            throw new CaseServerException("当前用例不存在", StatusCode.INTERNAL_ERROR);
        }

        return WebSocket.getRoom(false, BitBaseUtil.mergeLong(record.getId(), record.getCaseId()));
    }

    /**
     * ☆将当前record的操作记录和用例集的内容进行merge，返回合并后的内容
     */
    public JSONObject getData(MergeCaseDto dto) {
        String websocketCaseContent = null;
        if (dto.getRecordId() > 0L) {
            websocketCaseContent = WebSocket.getRoom(false, BitBaseUtil.mergeLong(dto.getRecordId(), dto.getCaseId())).getTestCaseContent();
        }

        String caseContent = caseMapper.selectOne(dto.getCaseId()).getCaseContent();
        JSONObject content = JSON.parseObject(caseContent);
        if (websocketCaseContent != null) {
            JSONObject websocketContent = JSON.parseObject(websocketCaseContent);
            long currentBase = websocketContent.getLong("base");
            content.put("base", currentBase);
        }

        // 如果不是全部圈选的圈选条件
        if (!StringUtils.isEmpty(dto.getChooseContent()) && !dto.getChooseContent().contains(OE_PICK_ALL)) {
            PickCaseDto pickCaseDto = JSON.parseObject(dto.getChooseContent(), PickCaseDto.class);

            // 似乎是想用BFS做广度遍历
            JSONObject caseRoot = content.getJSONObject("root");
            Stack<JSONObject> objCheck = new Stack<>();
            Stack<IntCount> iCheck = new Stack<>();
            objCheck.push(caseRoot);

            //获取对应级别用例
            if (!CollectionUtils.isEmpty(pickCaseDto.getPriority())) {
                TreeUtil.getPriority(objCheck, iCheck, caseRoot, pickCaseDto.getPriority());
            }
            if (!CollectionUtils.isEmpty(pickCaseDto.getResource())) {
                TreeUtil.getChosenCase(caseRoot, new HashSet<>(pickCaseDto.getResource()), "resource");
            }
        } else {
            // 给未来的环境选择做好打算...
            if (EnvEnum.TestQaEnv.getValue().equals(dto.getEnv()) || EnvEnum.TestRdEnv.getValue().equals(dto.getEnv())) {
                // 似乎是想用BFS做广度遍历
                JSONObject caseRoot = content.getJSONObject("root");
                Stack<JSONObject> objCheck = new Stack<>();
                Stack<IntCount> iCheck = new Stack<>();
                objCheck.push(caseRoot);

                // 这里就是默认圈选全部用例
                TreeUtil.getPriority0(objCheck, iCheck, caseRoot);
            }
        }
        //合并用例
        String recordContent = dto.getRecordContent();
        JSONObject recordObj = new JSONObject();

        if (StringUtils.isEmpty(recordContent)) {
            // 脏数据，不管
        } else if (recordContent.startsWith("[{")) {
            for (Object o : JSON.parseArray(recordContent)) {
                recordObj.put(((JSONObject) o).getString("id"), ((JSONObject) o).getLong("progress"));
            }
        } else {
            recordObj = JSON.parseObject(recordContent);
        }

        IntCount execCount = new IntCount(recordObj.size());
        TreeUtil.mergeExecRecord(content.getJSONObject("root"), recordObj, execCount);
        return TreeUtil.parse(content.toJSONString());
    }
}

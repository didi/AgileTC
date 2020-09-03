package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.*;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.ExecRecordService;
import com.xiaoju.framework.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by didi on 2019/9/23.
 */
@Slf4j
@Service
public class ExecRecordServiceImpl implements ExecRecordService {
    @Autowired
    private ExecRecordMapper execRecordMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;

    @Override
    public List<ExecRecord> getRecordByCaseId(Long caseId) {
        if (caseId == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "用例id不能为空");
        }
        List<ExecRecord> execRecordList = execRecordMapper.listRecordByCaseId(caseId);
        execRecordList = getCount(execRecordList);
        log.info(caseId + "get record list success.  " + execRecordList.toString());
        return execRecordList;
    }

    @Override
    public PageResult<ExecRecord> getRecordByRequirementId(Long requirementId, int pageNum, int pageSize, int channel) {

        PageResult<ExecRecord> pageResult = new PageResult<>();

        pageResult.setTotal(execRecordMapper.selectByRequirementTotal("" + requirementId, channel));

        int offset = (pageNum - 1) * pageSize;
        //分页查询
        List<ExecRecord> list = execRecordMapper.selectByRequirementId("" + requirementId, offset, pageSize, channel);

        list = getCount(list);

        pageResult.setData(list);

        return pageResult;
    }

    //获取通过率，测试用例数，总用例数
    private List<ExecRecord> getCount(List<ExecRecord> list) {

        for (ExecRecord e : list) {
            JSONObject object = getData(e);
            e.setTotalCount((Integer) object.get("totalCount"));
            try {
                String compareTime = "1971-01-01 00:00:00";
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date comparedate = df.parse(compareTime);
                if (e.getExpectEndTime().compareTo(comparedate) == 0) {
                    e.setExpectEndTime(null);
                }
                if (e.getExpectStartTime().compareTo(comparedate) == 0) {
                    e.setExpectStartTime(null);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            if (e.getTotalCount() != 0) {
                int success_count = object.getInteger("successCount");
                int pass_count = object.getInteger("passCount");
                int block_count = object.getInteger("blockCount");

                e.setPassCount(pass_count);
                e.setSuccessCount(success_count);
                e.setBlockCount(block_count);

                BigDecimal progressRate = new BigDecimal((double) pass_count * 100 / (double) e.getTotalCount());
                e.setProgressRate(progressRate.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                //bug数量暂取为失败数
                e.setBugNum(pass_count - success_count - block_count);

                BigDecimal passRate = new BigDecimal((double) success_count * 100 / (double) e.getTotalCount());
                //通过率=执行成功用例/总用例
                e.setPassRate(passRate.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            } else {
                e.setPassCount(0);
                e.setSuccessCount(0);
                e.setPassRate(0d);
                e.setProgressRate(0d);
            }

            e.setCaseContent("");
        }
        return list;
    }


    //获取记录详情，从testCase 与 record node拼装
    @Override
    public ExecRecord getRecordById(Long id) {
        if (id == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "记录id不能为空");
        }
        ExecRecord e = execRecordMapper.selectByPrimaryKey(id);
        try {
            String compareTime = "1971-01-01 00:00:00";
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date comparedate = df.parse(compareTime);
            if (e.getExpectEndTime().compareTo(comparedate) == 0) {
                e.setExpectEndTime(null);
            }
            if (e.getExpectStartTime().compareTo(comparedate) == 0) {
                e.setExpectStartTime(null);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        //实时取数据
        JSONObject object = getData(e);

        e.setTotalCount((Integer) object.get("totalCount"));

        if (e.getTotalCount() != 0) {
            int success_count = object.getInteger("successCount");
            int pass_count = object.getInteger("passCount");
            int block_count = object.getInteger("blockCount");

            e.setPassCount(pass_count);
            e.setSuccessCount(success_count);
            e.setBlockCount(block_count);

            BigDecimal progressRate = new BigDecimal((double) pass_count * 100 / (double) e.getTotalCount());
            e.setProgressRate(progressRate.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            //失败个数=执行-成功-阻塞
            e.setBugNum(pass_count - success_count - block_count);

            BigDecimal passRate = new BigDecimal((double) success_count * 100 / (double) e.getTotalCount());
            //通过率=执行成功用例/总用例
            e.setPassRate(passRate.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        } else {
            e.setPassCount(0);
            e.setSuccessCount(0);
            e.setPassRate(0d);
            e.setProgressRate(0d);
        }

        if (e == null) {
            log.error("get no record. id = " + id);
            return null;
        }
        log.info("get record sucess, record is " + e.toString());
        return e;
    }


    @Override
    public ExecRecord addRecord(ExecRecord execRecord) {
        if (execRecord == null || execRecord.getCaseId() == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "需求用例id不能为空");
        }
        if (StringUtils.isEmpty(execRecord.getCreator())) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "创建人不能为空");
        }
        if (StringUtils.isEmpty(execRecord.getTitle())) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "title不能为空");
        }
        if (execRecord.getExpectStartTime() != null & execRecord.getExpectEndTime() == null || execRecord.getExpectStartTime() == null & execRecord.getExpectEndTime() != null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "时间为区间");
        }
        if (execRecord.getEnv() != null) {
            List<ExecRecord> execRecordList = execRecordMapper.getEnvList(execRecord)
                    .stream().filter(p -> p.getEnv() == EnvEnum.TestQaEnv.getValue() || p.getEnv() == EnvEnum.TestRdEnv.getValue() || (p.getCreator().equals(execRecord.getCreator()))).collect(Collectors.toList());
            if (execRecordList.size() > 0) {
                throw new ResponseException(ErrorCode.COMMON_PARAM_ERROR, "当前环境类型已经创建过执行记录，请基于原case记录编辑。");
            }
        }

        String title = execRecord.getTitle();
        JSONObject jsonObject = getData(execRecord);
        execRecord.setTotalCount((Integer) jsonObject.get("totalCount"));

        //有预计完成时间区间时
        if (execRecord.getExpectStartTime() != null & execRecord.getExpectEndTime() != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                execRecord.setExpectStartTime(format.parse(format.format(execRecord.getExpectStartTime())));
                execRecord.setExpectEndTime(format.parse(format.format(execRecord.getExpectEndTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        execRecord.setTitle(title);

        if (!StringUtils.isEmpty(execRecord.getEnv())) {//老逻辑
            execRecord.setTitle(title + "-" + EnvEnum.transfer(execRecord.getEnv()).getName());
        }

        Long recordId = execRecordMapper.insert(execRecord);
        log.info("add record success. id = " + recordId);
        //todo: 增加通知机制
        return execRecord;
    }

    @Override
    public ExecRecord modifyTestRecord(ExecRecord execRecord) {
        if (execRecord == null || StringUtils.isEmpty(execRecord.getCaseContent())) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "内容不能为空");
        }
        execRecordMapper.updateById(execRecord);
        log.info("add record success. id = " + execRecord.getId());
        return null;
    }

    @Override
    public List<Integer> getEnv(Long caseId, String creator) {
        if (caseId == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "用例id不能为空");
        }
        if (StringUtils.isEmpty(creator)) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "创建人不能为空");
        }

        List<ExecRecord> recordList = execRecordMapper.listRecordByCaseId(caseId)
                .stream().filter(p -> (p.getCreator().equals(creator) || p.getEnv() == EnvEnum.TestRdEnv.getValue() || p.getEnv() == EnvEnum.TestQaEnv.getValue())).collect(Collectors.toList());

        List<Integer> envList = new ArrayList<>();
        envList.add(EnvEnum.TestEnv.getValue());
        envList.add(EnvEnum.PreEnv.getValue());
        envList.add(EnvEnum.OnlineEnv.getValue());
        envList.add(EnvEnum.TestQaEnv.getValue());
        envList.add(EnvEnum.TestRdEnv.getValue());

        for (ExecRecord record : recordList) {
            if (envList.indexOf(record.getEnv()) != -1) {
                envList.remove(record.getEnv());
            }
        }

        log.info("get env success. " + envList.toString());
        return envList;
    }

    @Override
    public void deleteRecord(Long caseId) {
        if (caseId == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "用例id不能为空");
        }
        log.warn("delete record. case id: " + caseId);
        execRecordMapper.deleteByCaseId(caseId);
    }

    @Override
    public ExecRecord clearRecord(ExecRecord execRecord) {
        if (execRecord == null || execRecord.getId() == null) {
            throw new ResponseException(ErrorCode.COMMON_NO_PARAM, "记录id不能为空");
        }
        ExecRecord existRecord = execRecordMapper.selectByPrimaryKey(execRecord.getId());
        log.info("clear record, case id: " + existRecord.getCaseId());
        existRecord.setCaseContent("");
        existRecord.setSuccessCount(0);
        existRecord.setPassCount(0);
        existRecord.setModifier(execRecord.getModifier());
        Date date = new Date(System.currentTimeMillis());
        existRecord.setGmtModified(date);
        execRecordMapper.updateById(existRecord);
        JSONObject jsonObject = getData(existRecord);
        existRecord.setCaseContent(jsonObject.get("content").toString());
        return existRecord;
    }

    @Override
    public int deleteRecordById(Long id) {
        int ret = -1;
        try {
            ret = execRecordMapper.deleteById(id);
            log.info("softdelete record " + id + "return message " + ret);
        } catch (Exception e) {
            log.error("update exception " + e);
        }
        return ret;
    }

    @Override
    public int editRecord(ExecRecord execRecord) {

        //有预计完成时间区间时
        if (execRecord.getExpectStartTime() != null & execRecord.getExpectEndTime() != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                execRecord.setExpectStartTime(format.parse(format.format(execRecord.getExpectStartTime())));
                execRecord.setExpectEndTime(format.parse(format.format(execRecord.getExpectEndTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            execRecord.setExpectEndTime(null);
            execRecord.setExpectEndTime(null);
        }

        Long id = execRecord.getId();
        ExecRecord oldRecord = getRecordById(id);

        Integer result = execRecordMapper.editRecord(execRecord);
        log.info("edit record success. id = " + id);
        //todo: 增加通知机制

        return result;
    }

    public JSONObject getData(ExecRecord execRecord) {
        Integer env = execRecord.getEnv();
        String caseContent = testCaseMapper.selectByPrimaryKey(execRecord.getCaseId()).getCaseContent();
        JSONObject content;
        //有圈选条件-根据圈选条件取用例(有具体圈选条件)
        if (execRecord.getChooseContent() != null && !execRecord.getChooseContent().equals("") && !execRecord.getChooseContent().contains("\"priority\":[\"0\"]")) {
            Map<String, List<String>> chosen = JSON.parseObject(execRecord.getChooseContent(), Map.class);
            content = JSON.parseObject(caseContent);
            JSONObject caseRoot = content.getJSONObject("root");
            Stack<JSONObject> objCheck = new Stack<>();

            Stack<IntCount> iCheck = new Stack<>();
            objCheck.push(caseRoot);
            //获取对应级别用例
            List<String> priority = chosen.get("priority");
            List<String> resource = chosen.get("resource");
            if (priority != null && priority.size() > 0)
                TreeUtil.getPriority(objCheck, iCheck, caseRoot, priority);
            if (resource != null && resource.size() > 0)
                TreeUtil.getChosenCase(caseRoot, new HashSet<>(resource), "resource");
        } else {//环境选择
            if (env == EnvEnum.TestQaEnv.getValue() || env == EnvEnum.TestRdEnv.getValue()) {
                content = JSON.parseObject(caseContent);
                JSONObject caseRoot = content.getJSONObject("root");
                Stack<JSONObject> objCheck = new Stack<>();

                Stack<IntCount> iCheck = new Stack<>();
                objCheck.push(caseRoot);
                TreeUtil.getPriority0(objCheck, iCheck, caseRoot);

                log.info("get smk case: " + content.toJSONString());
            } else {
                content = JSON.parseObject(caseContent);
            }
        }
        //合并用例
        String record = execRecord.getCaseContent();
        JSONObject recordObj = new JSONObject();
        if (record == null || record.equals("")) {
            log.info("record not exist");
        } else if (record.startsWith("[{")) {
            JSONArray jsonArray = JSON.parseArray(record);
            for (Object o : jsonArray) {
                recordObj.put(((JSONObject) o).getString("id"), ((JSONObject) o).getLong("progress"));
            }
        } else if (recordObj.containsKey("root")) {
            log.warn("current is old record, need to parse first.");
            recordObj = TreeUtil.parse(record).getJSONObject("progress");
            execRecord.setCaseContent(recordObj.toJSONString());
            modifyTestRecord(execRecord);
        } else {
            recordObj = JSON.parseObject(record);
        }

        IntCount ExecCount = new IntCount(recordObj.size());
        TreeUtil.mergeExecRecord(content.getJSONObject("root"), recordObj, ExecCount);
        caseContent = content.toJSONString();

        log.info("getRecordDate case: " + caseContent);
        JSONObject caseData = TreeUtil.parse(caseContent);
        return caseData;
    }
}

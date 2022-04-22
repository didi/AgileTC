package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.util.TreeUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class RecordEntity extends RoomEntity {
    Long recordId;
    ExecRecord execRecord;

    public RecordEntity(String roomId, Long caseId, TestCaseMapper caseMapper, Long recordId, ExecRecordMapper execRecordMapper) {
        super(roomId, caseId, caseMapper);
        this.recordId = recordId;
        String recordContent = mergeRecord(recordId, testCase.getCaseContent(), execRecordMapper);
        testCase.setCaseContent(recordContent);
        testCase.setGroupId(recordId);
    }

    private String mergeRecord(Long recordId, String caseContentStr, ExecRecordMapper execRecordMapper) {

        String retCaseContent;

        ExecRecord record = execRecordMapper.selectOne(recordId);
        if (record == null) {
            //todo: 在controller层应该已经创建了任务，因此这里一定不为空
            LOGGER.error(Thread.currentThread().getName() + ": 当前用例执行者初次打开任务");
        }

        String recordContent = record.getCaseContent();
        JSONObject recordObj = new JSONObject();
        if (StringUtils.isEmpty(recordContent)) {
            // 其实当前任务还没有任何执行记录
            LOGGER.info(Thread.currentThread().getName() + ": first create record.");
        } else if (recordContent.startsWith("[{")) {
            JSONArray jsonArray = JSON.parseArray(recordContent);
            for (Object o : jsonArray) {
                recordObj.put(((JSONObject) o).getString("id"), ((JSONObject) o).getLong("progress"));
            }
        } else {
            recordObj = JSON.parseObject(recordContent);
        }

        IntCount ExecCount = new IntCount(recordObj.size());
        // 如果当前record是圈选了部分的圈选用例
        if (!StringUtils.isEmpty(record.getChooseContent()) && !record.getChooseContent().contains("\"priority\":[\"0\"]")) {
            Map<String, List<String>> chosen = JSON.parseObject(record.getChooseContent(), Map.class);

            JSONObject caseContent = JSON.parseObject(caseContentStr);
            JSONObject caseRoot = caseContent.getJSONObject("root");
            Stack<JSONObject> objCheck = new Stack<>();

            Stack<IntCount> iCheck = new Stack<>();
            objCheck.push(caseRoot);

            List<String> priority = chosen.get("priority");
            List<String> resource = chosen.get("resource");
            //获取对应级别用例
            if (!CollectionUtils.isEmpty(priority)) {
                TreeUtil.getPriority(objCheck, iCheck, caseRoot, priority);
            }
            if (!CollectionUtils.isEmpty(resource)) {
                TreeUtil.getChosenCase(caseRoot, new HashSet<>(resource), "resource");
            }

            TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
            retCaseContent = caseContent.toJSONString();
        } else {
            // 如果是全部的，那么直接把testcase 给 merge过来
            JSONObject caseContent = JSON.parseObject(caseContentStr);
            TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
            retCaseContent = caseContent.toJSONString();
        }
        return retCaseContent;
    }
}

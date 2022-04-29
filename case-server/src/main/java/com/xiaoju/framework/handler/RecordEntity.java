package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.SocketIOClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.util.TreeUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.xiaoju.framework.constants.SystemConstant.COMMA;

public class RecordEntity extends RoomEntity {
    Long recordId;
//    ExecRecord execRecord;
    ExecRecordMapper recordMapper;

    public RecordEntity(String roomId, Long caseId, TestCaseMapper caseMapper, Long recordId, ExecRecordMapper execRecordMapper) {
        super(roomId, caseId, caseMapper);
        this.recordId = recordId;
        String recordContent = mergeRecord(recordId, testCase.getCaseContent(), execRecordMapper);
        testCase.setCaseContent(recordContent);
        testCase.setGroupId(recordId);
        this.recordMapper = execRecordMapper;

    }

    @Override
    public void removeClient(SocketIOClient client) {
        this.clientMap.remove(client.getSessionId());
        LOGGER.info("remove client, current user number:" + this.clientMap.size());
        testCase.setGmtModified(new Date(System.currentTimeMillis()));
        String user = client.getHandshakeData().getSingleUrlParam("user");
        ExecRecord record = recordMapper.selectOne(Long.valueOf(client.getHandshakeData().getSingleUrlParam("recordId")));
        if (record == null) {
            throw new CaseServerException("执行任务不存在", StatusCode.NOT_FOUND_ENTITY);
        }
        JSONObject jsonObject = TreeUtil.parse(testCase.getCaseContent());
        JSONObject jsonProgress = jsonObject.getJSONObject("progress");
        Integer totalCount = jsonObject.getInteger("totalCount");
        Integer passCount = jsonObject.getInteger("passCount");
        Integer failCount = jsonObject.getInteger("failCount");
        Integer blockCount = jsonObject.getInteger("blockCount");
        Integer successCount = jsonObject.getInteger("successCount");
        Integer ignoreCount = jsonObject.getInteger("ignoreCount");

        List<String> names = Arrays.stream(record.getExecutors().split(COMMA)).filter(e->!StringUtils.isEmpty(e)).collect(Collectors.toList());
        long count = names.stream().filter(e -> e.equals(user)).count();

        if (count > 0) {
            // 有重合，不管了
            ;
        } else {
            // 没重合往后面塞一个
            names.add(user);
        }

        record.setExecutors(String.join(",", names));
        record.setModifier(user);
        record.setGmtModified(new Date(System.currentTimeMillis()));
        record.setCaseContent(jsonProgress.toJSONString());
        record.setFailCount(failCount);
        record.setBlockCount(blockCount);
        record.setIgnoreCount(ignoreCount);
        record.setPassCount(passCount);
        record.setTotalCount(totalCount);
        record.setSuccessCount(successCount);
        recordMapper.update(record);

        LOGGER.info(Thread.currentThread().getName() + ": 数据库用例记录更新。record: " + record.getCaseContent());
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

package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.entity.dto.RecordWsDto;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.util.BitBaseUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Created by didi on 2021/3/27.
 */
public class RecordRoom extends Room {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordRoom.class);
    private long recordId;
    private ExecRecord recordUpdate;
    private String executors;
    public RecordRoom(Long id) {
        super(id);
        recordId = BitBaseUtil.getHigh32(id);
    }

    @Override
    public Player createAndAddPlayer(Client client) {
        Player p = super.createAndAddPlayer(client);
        // 合并record和用例后发送给前端
        String caseContent = testCaseContent != null ? testCaseContent : testCase.getCaseContent();
        if (p.getRoom().players.size() <= 1) {
            mergeRecoed(p.getClient().getRecordId(), caseContent);
        }
        p.getClient().sendMessage(testCaseContent);
        LOGGER.info(Thread.currentThread().getName() + ": 新的用户加入成功，传输用例内容： " + testCaseContent);
        return p;
    }

    @Override
    protected void internalRemovePlayer(Player p) {
        super.internalRemovePlayer(p);

        // 如果是最后一个用户离开，需要关闭广播任务
        if (players.size() == 0) {
            if (testCaseContent != null) {
                LOGGER.info(Thread.currentThread().getName() + ": 当前的用例内容是：" + testCaseContent);
//                testCase.setCaseContent(testCaseContent);
                testCase.setGmtModified(new Date(System.currentTimeMillis()));

                JSONObject jsonObject = TreeUtil.parse(testCaseContent);
                JSONObject jsonProgress = jsonObject.getJSONObject("progress");
                Integer totalCount = jsonObject.getInteger("totalCount");
                Integer passCount = jsonObject.getInteger("passCount");
                Integer failCount = jsonObject.getInteger("failCount");
                Integer blockCount = jsonObject.getInteger("blockCount");
                Integer successCount = jsonObject.getInteger("successCount");
                Integer ignoreCount = jsonObject.getInteger("ignoreCount");

                ExecRecord recordUpdate = new ExecRecord();
                recordUpdate.setId(Long.valueOf(recordId));

                String[] list = executors.split(SystemConstant.COMMA);
                if (list.length == 0) {
                    executors = p.getClient().getClientName();
                } else {
                    if (!Arrays.asList(list).contains(p.getClient().getClientName())) {
                        executors = new String(executors + SystemConstant.COMMA + p.getClient().getClientName());
                    }
                }

                recordUpdate.setExecutors(executors);
                recordUpdate.setModifier(p.getClient().getClientName());
                recordUpdate.setGmtModified(new Date(System.currentTimeMillis()));
                recordUpdate.setCaseContent(jsonProgress.toJSONString());
                recordUpdate.setFailCount(failCount);
                recordUpdate.setBlockCount(blockCount);
                recordUpdate.setIgnoreCount(ignoreCount);
                recordUpdate.setPassCount(passCount);
                recordUpdate.setTotalCount(totalCount);
                recordUpdate.setSuccessCount(successCount);
                recordService.modifyRecord(recordUpdate);
                LOGGER.info(Thread.currentThread().getName() + ": 数据库用例内容更新。");
            }
            LOGGER.info(Thread.currentThread().getName() + ": 最后一名用户 " + p.getClient().getClientName() + " 离开，关闭。");
        }

        // 广播有用户离开
        broadcastRoomMessage("当前用户数:" + players.size() + "。用例编辑者 " + p.getClient().getClientName() + " 离开");
    }

    public void mergeRecoed(Long recordId, String caseContentStr) {
        RecordWsDto dto = recordService.getWsRecord(recordId);
        if (dto == null) {
            //todo: 在controller层应该已经创建了任务，因此这里一定不为空
            assert false;
            LOGGER.error(Thread.currentThread().getName() + ": 当前用例执行者初次打开任务");
        }
        executors = dto.getExecutors();
        String recordContent = dto.getCaseContent();
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
        if (!StringUtils.isEmpty(dto.getChooseContent()) && !dto.getChooseContent().contains("\"priority\":[\"0\"]")) {
            Map<String, List<String>> chosen = JSON.parseObject(dto.getChooseContent(), Map.class);

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
            testCaseContent = caseContent.toJSONString();
        } else {
            // 如果是全部的，那么直接把testcase 给 merge过来
            JSONObject caseContent = JSON.parseObject(caseContentStr);
            TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
            testCaseContent = caseContent.toJSONString();
        }
    }
}

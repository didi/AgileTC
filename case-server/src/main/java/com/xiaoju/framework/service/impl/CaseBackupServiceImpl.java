package com.xiaoju.framework.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.flipkart.zjsonpatch.DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE;

/**
 * 备份记录
 *
 * @author didi
 * @date 2020/11/5
 */
@Service
public class CaseBackupServiceImpl implements CaseBackupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseBackupServiceImpl.class);

    @Resource
    private CaseBackupMapper caseBackupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized CaseBackup insertBackup(CaseBackup caseBackup) {
        LOGGER.info(Thread.currentThread().getName() + ": 备份保存当前用例。");

        // 此处可以与最新的内容比对，如果一致，则不更新backup表，减少版本数量
        List<CaseBackup> caseBackups = caseBackupMapper.selectByCaseId(caseBackup.getCaseId(), null, null);

        // 如果当前已有，则直接返回
        // todo 此处还是用版本信息控制更加合理
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            if (caseBackups.size() > 0 &&
                    JsonDiff.asJson(jsonMapper.readTree(caseBackups.get(0).getCaseContent()),
                            jsonMapper.readTree(caseBackup.getCaseContent())).size() == 0 &&
                    (!StringUtils.isEmpty(caseBackups.get(0).getRecordContent())) &&
                    JsonDiff.asJson(jsonMapper.readTree(caseBackups.get(0).getRecordContent()),
                            jsonMapper.readTree(caseBackup.getRecordContent())).size() == 0) {
                LOGGER.info("当前内容已经保存过了，不再重复保存。");
                return caseBackups.get(0);
            }
        } catch (IOException e) {
            LOGGER.info("json转换异常. 数据继续备份", e);
        }

        int ret = caseBackupMapper.insert(caseBackup);
        if (ret < 1) {
            LOGGER.error("用例备份落库失败. casebackup id: " + caseBackup.getCaseId() + ", case content: " +
                    caseBackup.getCaseContent() + ", record: " + caseBackup.getRecordContent());
            return null;
        }

        LOGGER.info("备份保存当前用例。caseid:" + caseBackup.getCaseId());

        return caseBackup;
    }

    @Override
    public JsonNode getCaseDiff(Long backupId2, Long backupId1) {
        ObjectMapper jsonMapper = new ObjectMapper();
        CaseBackup caseBackup1 = caseBackupMapper.selectByBackupId(backupId1);
        CaseBackup caseBackup2 = caseBackupMapper.selectByBackupId(backupId2);

        try {
            JsonNode content1 = jsonMapper.readTree(caseBackup1.getCaseContent());
            JsonNode content2 = jsonMapper.readTree(caseBackup2.getCaseContent());
            ArrayNode patches = (ArrayNode) JsonDiff.asJson(content1, content2, EnumSet.of(ADD_ORIGINAL_VALUE_ON_REPLACE));

            JsonNodeFactory FACTORY = JsonNodeFactory.instance;
            ArrayNode patchesNew = FACTORY.arrayNode();
            ObjectNode retJson = FACTORY.objectNode();
            Iterator<JsonNode> it = patches.elements();

            while (it.hasNext()) {
                JsonNode element = it.next();
                String op = element.get("op").textValue();
                if (op.equals("replace")) {
                    if (element.get("path").textValue().endsWith("base")) {
                        continue;
                    }
                    ObjectNode node1 = element.deepCopy();

                    if(!element.get("path").textValue().endsWith("image") && !element.get("path").textValue().contains("imageSize")) {
                        node1.remove("value");
                        node1.put("value", "旧内容：" + element.get("fromValue").textValue() + "\n新内容：" + element.get("value").textValue());
                    }
                    patchesNew.add(node1);
                    ObjectNode node2 = FACTORY.objectNode();
                    node2.put("op", "add");
                    String srcPath = element.get("path").textValue();

                    node2.put("path", srcPath.substring(0, srcPath.lastIndexOf('/')) + "/background");
                    node2.put("value", "#d6f0ff");
                    patchesNew.add(node2);
                } else if (op.equals("add")) {
                    ObjectNode node1 = element.deepCopy();
                    traverse(node1, "add");

                    patchesNew.add(node1);

                } else if (op.equals("remove")) {
                    patchesNew.add(element);
                    ObjectNode node1 = element.deepCopy();
                    node1.remove("op");
                    node1.put("op", "add");
                    traverse(node1, "remove");
                    patchesNew.add(node1);

                } else {
                    LOGGER.info("op is: " + element.toString());
                }
            }
            JsonNode target = JsonPatch.apply((JsonNode) patchesNew, content1);

            retJson.set("content", target);
            ArrayNode cardJson = FACTORY.arrayNode();
            ObjectNode backup1 = FACTORY.objectNode();
            backup1.put("user", caseBackup1.getCreator());
            backup1.put("time", caseBackup1.getGmtCreated().toString());
            ObjectNode backup2 = FACTORY.objectNode();
            backup2.put("user", caseBackup2.getCreator());
            backup2.put("time", caseBackup2.getGmtCreated().toString());
            cardJson.add(backup1);
            cardJson.add(backup2);

            retJson.set("backupinfo", cardJson);

            return retJson;

        } catch (Exception e) {
            LOGGER.error("json mapper read tree exception. ", e);
            return null;
        }

    }

    private void traverse(JsonNode node, String op) {
        Iterator<JsonNode> iterator = node.iterator();

        while (iterator.hasNext()) {
            JsonNode n = iterator.next();
            if (n.size() > 0) {
                if (n.has("id")) {
                    if (op.equals("add")) {
                        ((ObjectNode) n).put("background", "#ddfade");
                    } else {
                        ((ObjectNode) n).put("background", "#ffe7e7");
                    }
                }
                traverse(n, op);
            } else {
//                 System.out.println(n.toString());
            }
        }

    }
    @Override
    public List<CaseBackup> getBackupByCaseId(Long caseId, String beginTime, String endTime) {
        List<CaseBackup> backupsSrc = caseBackupMapper.selectByCaseId(caseId, transferTime(beginTime), transferTime(endTime));
        List<CaseBackup> backups = new ArrayList<>();
        String pattern = "\"base\":(\\d+).*";
        Pattern r = Pattern.compile(pattern);
        Integer compareVersion = 0;
        for (CaseBackup cb:backupsSrc) {
            Matcher m = r.matcher(cb.getCaseContent());
            if (m.find()) {
                Integer currentVersion = Integer.valueOf(m.group(1));
                if (!currentVersion.equals(compareVersion)) {
                    backups.add(cb);
                    compareVersion = currentVersion;
                } else {
                    LOGGER.error("base信息一致。过滤信息。base： " + currentVersion);
                }
            } else {
                LOGGER.error("未找到base信息。用例内容是：" + cb.getCaseContent());
            }
        }
        return backups;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBackup(Long caseId ) {
        return caseBackupMapper.updateByCaseId(caseId);
    }

    private Date transferTime(String time) {
        if (time == null) {
            return null;
        }
        return TimeUtil.transferStrToDateInSecond(time);
    }

    public int insertEditInfo(CaseBackup caseBackup) {
        return caseBackupMapper.insert(caseBackup);
    }
}

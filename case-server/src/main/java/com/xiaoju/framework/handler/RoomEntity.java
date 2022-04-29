package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.util.TreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class RoomEntity {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RoomEntity.class);
    String roomId;
    Long caseId;
    Map<UUID, String> clientMap;
    TestCase testCase;
    TestCaseMapper caseMapper;
    SocketIOClient lockClient;
    ObjectMapper jsonMapper;
    JsonNodeFactory FACTORY;
    ReentrantLock lock;

    public RoomEntity(String roomId, Long caseId, TestCaseMapper caseMapper) {
        this.roomId = roomId;
        this.caseMapper = caseMapper;
        this.caseId = caseId;
        this.testCase = caseMapper.selectOne(caseId);
        this.clientMap = new HashMap<>();
        this.jsonMapper = new ObjectMapper();
        this.FACTORY = JsonNodeFactory.instance;
        String res = testCase == null ? null : testCase.getCaseContent();
        if (StringUtils.isEmpty(res)) {
            LOGGER.error(Thread.currentThread().getName() + ": 用例内容为空");
            // todo: 此处需要走异常处理流程
        } else {
            LOGGER.info("case content is: " + testCase.getCaseContent());
        }
    }

    public void addClient(SocketIOClient client) {

        this.clientMap.put(client.getSessionId(), client.getHandshakeData().getSingleUrlParam("user"));
        LOGGER.info("add client, current user number:" + this.clientMap.size() + ", name: " + client.getHandshakeData().getSingleUrlParam("user"));

    }

    public void removeClient(SocketIOClient client) {
        this.clientMap.remove(client.getSessionId());
        LOGGER.info("remove client, current user number:" + this.clientMap.size());
        if (this.clientMap.size() == 0) {
            try {
                TestCase testCaseBase = caseMapper.selectOne(caseId);
                JsonNode saveContent = jsonMapper.readTree(testCase.getCaseContent());
                JsonNode baseContent = jsonMapper.readTree(testCaseBase.getCaseContent());
                if (saveContent.get("base").asInt() > baseContent.get("base").asInt()) {
                    // 保存落库
                    TestCase testCaseToUpdate = this.testCase;
                    TreeUtil.caseDFSValidate(saveContent.get("root"));
                    testCaseToUpdate.setCaseContent(saveContent.toString());
                    testCaseToUpdate.setGmtModified(new Date(System.currentTimeMillis()));
                    testCaseToUpdate.setModifier(client.getHandshakeData().getSingleUrlParam("user"));
                    int ret = caseMapper.update(testCaseToUpdate);
                    if (ret < 1) {
                        LOGGER.error(Thread.currentThread().getName() + ": 数据库用例内容更新失败。 ret = " + ret);
                        LOGGER.error("应该保存的用例内容是：" + saveContent);
                    }
                } else {
                    // 不保存
                    LOGGER.info(Thread.currentThread().getName() + "不落库." + saveContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getClientNum() {
        return this.clientMap.size();
    }

    public boolean isLockedByClient() {
        return this.lockClient != null;
    }

    public void clientLock(SocketIOClient client) {
        this.lockClient = client;
    }

    public boolean lockByClient(SocketIOClient client) {
        return this.lockClient == client;
    }


    public void clientUnlock() {
        this.lockClient = null;
    }

    public void lock() {
//        this.lock.lock();
    }

    public void unlock() {
//        this.lock.unlock();
    }

    public String getRoomId() {
        return this.roomId;
    }

    public String getClientName() {

        return clientMap.values().stream().collect(Collectors.joining(","));
    }

    public Long getCaseId() {
        return this.caseId;
    }
    public String getCaseContent() {
        return testCase.getCaseContent();
    }

    public void setCaseContent(String caseContent) {
        this.testCase.setCaseContent(caseContent);
    }
}

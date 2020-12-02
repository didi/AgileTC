package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.config.ApplicationConfig;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.RecordWsDto;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.RecordService;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 协同类
 *
 * @author didi
 * @date 2020/9/23
 */
@Component
@ServerEndpoint(value = "/api/case/{caseId}/{recordId}/{isCore}/{user}")
public class WebSocket {

    /**
     * 常量
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocket.class);

    private static final String PING_MESSAGE = "ping ping ping";

    private static final String PONG_MESSAGE = "pongpongpong";

    private static final String UNDEFINED = "undefined";

    /**
     * 依赖
     * @see ApplicationConfig#setWebsocketService(com.xiaoju.framework.service.RecordService, com.xiaoju.framework.mapper.TestCaseMapper)
     */
    public static RecordService recordService;
    public static TestCaseMapper caseMapper;

    /**
     * 在Websocket.class粒度下，存储所有的websocket信息
     * { buildSerial(caseId, recordId, sessionId), new Websocket() }
     */
    public static ConcurrentHashMap<String, WebSocket> webSocket = new ConcurrentHashMap<>();

    /**
     * 在Websocket.class粒度下，存储所有的websocket.key,主要用户方便获取用户信息
     * [buildSerial(caseId, recordId, sessionId)]
     */
    public static CopyOnWriteArrayList<String> keys = new CopyOnWriteArrayList<>();

    /**
     * 单机模式下可以使用公平锁, 对数据的访问和获取都做一次顺序拦截
     */
    private static ReentrantLock lock = new ReentrantLock(true);

    /**
     * 每个websocket所持有的基本信息
     */
    private String caseId;
    private Session session;
    private String caseContent;
    private long updateCaseTime;
    private long updateRecordTime;
    private String recordId;
    private String isCore;
    private String user;
    private long pongTimeStamp;

    @Override
    public String toString() {
        return String.format("[Websocket Info][%s]caseId=%s, sessionId=%s, recordId=%s, isCoreCase=%s",
                recordId == null || UNDEFINED.equals(recordId) ? "测试用例" : "执行任务", caseId, session.getId(), recordId, isCore
        );
    }

    public String currentSession() {
        return buildSerial(caseId, recordId, session.getId());
    }


    static {
        // 线程池，每过5s向所有session发送ping，如果6s内没有收到响应，会执行session.close()去关闭session
        LOGGER.info("[线程池执行ping-pong] time = {}", System.currentTimeMillis());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 3,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.execute(() -> {
            try {
                // 看看是不是链接关闭了，如果没有收到就关闭
                while (true) {
                    for (Map.Entry<String, WebSocket> entry : WebSocket.webSocket.entrySet()) {
                        if (!UNDEFINED.equals(entry.getValue().caseId)) {
                            // 其实这里可以把方法变成static
                            entry.getValue().singleSendMessage(entry.getValue().session, PING_MESSAGE);
                            // 看看是不是过时的内容，超过10秒无响应认为掉线
                            if (System.currentTimeMillis() - entry.getValue().pongTimeStamp > 10000) {
                                LOGGER.error("[线程池执行ping-pong出错]准备关闭当前websocket={}", entry.getValue().toString());
                                entry.getValue().onClose();
                            }
                        }
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                LOGGER.error("[线程池执行ping-pong出错]错误原因e={}", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static String buildSerial(String ... ids) {
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            builder.append(fill(id));
        }
        return builder.toString();
    }

    @OnOpen
    public void onOpen(@PathParam(value = "caseId") String caseId,
                                    @PathParam(value = "recordId") String recordId,
                                    @PathParam(value = "isCore") String isCore,
                                    @PathParam(value = "user") String user,
                                    Session session) throws IOException {
        this.session = session;
        this.caseId = caseId;
        this.recordId = recordId;
        this.isCore = isCore;
        this.user = user;
        this.updateCaseTime = 0;
        this.updateRecordTime = 0;
        this.pongTimeStamp = System.currentTimeMillis();
        LOGGER.info("[websocket-onOpen 开启新的session][{}]", toString());

        // 连基本的任务都不是，直接报错
        if (UNDEFINED.equals(caseId)) {
            throw new CaseServerException("用例id为空", StatusCode.WS_UNKNOWN_ERROR);
        }

        // 查看当前是否有其他用户一齐正在编辑此用例/任务
        // 如果有，那么打开的时候先保存一下
        if (getAllSessionInfo(caseId, recordId).size() >= 1) {
            saveCaseOrRecord(caseId, recordId, user);
        }

        // 然后再把当前用户信息装入
        lock.lock();
        try {
            // 这么做是因为使用了.startsWith，这里给与对齐，防止例如1+11和111是一样的情况
            String serial = currentSession();
            WebSocket.keys.add(serial);
            WebSocket.webSocket.put(serial, this);
        } finally {
            lock.unlock();
        }

        open(caseId, recordId, isCore);
    }

    @OnClose
    public void onClose() {
        LOGGER.info("[websocket-onClose 关闭当前session成功]当前session={}", currentSession());
        if (UNDEFINED.equals(caseId)) {
            throw new CaseServerException("用例id为空", StatusCode.WS_UNKNOWN_ERROR);
        }

        saveCaseOrRecord(caseId, recordId, user);
        lock.lock();
        try {
            String serial = currentSession();
            WebSocket.webSocket.remove(serial, this);
            WebSocket.keys.remove(serial);
        } finally {
            lock.unlock();
        }
    }

    @OnMessage(maxMessageSize = 1048576)
    public void onMessage(String message, Session session) throws IOException {
        // 线程池通信内容忽略
        if (message.contains(PONG_MESSAGE)) {
            pongTimeStamp = System.currentTimeMillis();
            return;
        }

        // 如果内容体为空，发送错误消息并且忽略
        if (null == WebSocket.webSocket.get(buildSerial(caseId, recordId, session.getId()))) {
            singleSendMessage(session, StatusCode.WS_UNKNOWN_ERROR.getCode());
            return;
        }

        JSONObject request = JSON.parseObject(message);

        // 没有patch就不要触发保存
        if (StringUtils.isEmpty(request.getString("patch"))) {
            return;
        }

        JSONArray patch = (JSONArray) request.get("patch");
        long currentVersion = ((JSONObject) request.get("case")).getLong("base");
        String msg2Other = patch.toJSONString().replace("[[{", "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (currentVersion + 1) + "},{");
        String msg2Own = "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (currentVersion + 1) + "}]]";
        // 发送给别人 也发送给自己
        batchSendMessage(getOthersSessionInfo(caseId, recordId, session.getId()), msg2Other);
        singleSendMessage(session, msg2Own);
        caseContent = ((JSONObject) request.get("case")).toJSONString().replace("\"base\":" + currentVersion, "\"base\":" + (currentVersion + 1));

        // 判断是用例还是任务
        if (patch.toJSONString().contains("/progress")) {
            // 如果是任务的修改，那么更新任务时间
            updateRecordTime = System.currentTimeMillis();
        } else {
            // 用例修改更新用例时间
            updateCaseTime = System.currentTimeMillis();
        }
    }

    @OnError
    public void onError(Session session, Throwable e) throws IOException {
        LOGGER.info("[websocket-onError 会话出现异常]当前session={}, 原因={}", currentSession(), e.getMessage());
        e.printStackTrace();

        // 给一个机会去触发当前内容的保存, 如果不是最新的，也不会触发保存，会被pass掉
        saveCaseOrRecord(caseId, recordId, user);
        lock.lock();
        try {
            String serial = currentSession();
            WebSocket.webSocket.remove(serial, this);
            WebSocket.keys.remove(serial);
        } finally {
            lock.unlock();
        }

        singleSendMessage(session, StatusCode.WS_UNKNOWN_ERROR.getCode());
    }

    /**
     * 给指定session发送消息
     */
    private void singleSendMessage(Session s, String message) throws IOException {
        if (s != null && s.isOpen()) {
            s.getBasicRemote().sendText(message);
        }
    }

    /**
     * 批量发送消息
     */
    private void batchSendMessage(List<String> keys, String message) throws IOException {
        for (String key : keys) {
            singleSendMessage(WebSocket.webSocket.get(key).session, message);
        }
    }

    /**
     * 获取当前case/record下的所有用户
     */
    private List<String> getAllSessionInfo(String caseId, String recordId) {
        return WebSocket.keys.stream().filter(key -> key.startsWith(buildSerial(caseId, recordId))).collect(Collectors.toList());
    }

    /**
     * 获取当前case/record下的其他用户
     */
    private List<String> getOthersSessionInfo(String caseId, String recordId, String sessionId) {
        List<String> keysRet = getAllSessionInfo(caseId, recordId);
        return keysRet.stream().filter(key -> !key.equals(buildSerial(caseId, recordId, sessionId))).collect(Collectors.toList());
    }

    /**
     * 根据recordId是否为undefined判断为更新任务还是用例
     * @see #onOpen(String, String, String, String, Session)
     * @see #onClose()
     * @see #onError(Session, Throwable)
     */
    private void saveCaseOrRecord(String caseId, String recordId, String user) {
        if (UNDEFINED.equals(recordId)) {
            saveCase(caseId);
        } else {
            saveRecord(caseId, recordId, user);
        }
    }

    /**
     * 保存用例
     * @see #saveCaseOrRecord(String, String, String)
     */
    private void saveCase(String caseId) {
        TestCase testCase = new TestCase();
        testCase.setId(Long.valueOf(caseId));
        // 获取当前用力下，所有的session串
        List<String> keys = getAllSessionInfo(caseId, UNDEFINED);
        long maxTime = 0;
        String keySave = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonContent = new JSONObject();

        // 将用例内容更新为最新
        for (String key : keys) {
            if (WebSocket.webSocket.get(key).updateCaseTime > maxTime) {
                maxTime = WebSocket.webSocket.get(key).updateCaseTime;
                jsonObject = TreeUtil.parse(WebSocket.webSocket.get(key).caseContent);
                jsonContent = jsonObject.getJSONObject("content");
                keySave = key;
            }
        }

        if (StringUtils.isEmpty(keySave)) {
            // 无需更新
            return;
        }

        //对比用例http更新时间和socket更新时间
        TestCase dbCase = caseMapper.selectOne(Long.valueOf(caseId));
        long tcUpdateTime = dbCase.getGmtModified().getTime();
        long wsTcUpdateTime = WebSocket.webSocket.get(keySave).updateCaseTime;

        //数据库更新时间大于socket最大更新时间则不需要保存
        if (tcUpdateTime >= wsTcUpdateTime) {
            for (String key : keys) {
                WebSocket.webSocket.get(key).caseContent = testCase.getCaseContent();
                WebSocket.webSocket.get(key).updateCaseTime = 0L;
            }
            return;
        } else {
            LOGGER.info("[Websocket case-save]当前内容没有保存上, 内容:{}, tcUpdateTime:{}, wsTcUpdateTime:{}",
                    WebSocket.webSocket.get(keySave).caseContent, tcUpdateTime, wsTcUpdateTime);
        }

        // 过来的内容没有content就不要保存了
        if (!jsonObject.containsKey("content")) {
            return;
        }

        // 更新所有的用户的caseContent
        for (String key : keys) {
            WebSocket.webSocket.get(key).caseContent = WebSocket.webSocket.get(keySave).caseContent;
            WebSocket.webSocket.get(key).updateCaseTime = 0L;
        }

        testCase.setCaseContent(jsonContent.toJSONString());
        testCase.setGmtModified(new Date(wsTcUpdateTime));
        caseMapper.update(testCase);
    }

    /**
     * 保存任务
     * @see #saveCaseOrRecord(String, String, String)
     */
    private void saveRecord(String caseId, String recordId, String user) {
        List<String> keys = getAllSessionInfo(caseId, recordId);
        long maxTime = 0;
        String keySave = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonProgress = new JSONObject();
        int totalCount = 0;
        int passCount = 0;
        int successCount = 0;
        int failCount = 0;
        int blockCount = 0;
        int ignoreCount = 0;
        // 将用例内容更新为最新
        for (String key : keys) {
            if (WebSocket.webSocket.get(key).updateRecordTime > maxTime) {
                maxTime = WebSocket.webSocket.get(key).updateRecordTime;
                jsonObject = TreeUtil.parse(WebSocket.webSocket.get(key).caseContent);
                jsonProgress = jsonObject.getJSONObject("progress");
                totalCount = jsonObject.getInteger("totalCount");
                passCount = jsonObject.getInteger("passCount");
                failCount = jsonObject.getInteger("failCount");
                blockCount = jsonObject.getInteger("blockCount");
                successCount = jsonObject.getInteger("successCount");
                ignoreCount = jsonObject.getInteger("ignoreCount");
                keySave = key;
            }
        }

        if (StringUtils.isEmpty(keySave)) {
            return;
        }

        //获取数据库更新时间
        RecordWsDto dto = recordService.getWsRecord(Long.parseLong(recordId));
        long recordUpdateTime = dto.getUpdateTime().getTime();
        long wsUpdateTime = WebSocket.webSocket.get(keySave).updateRecordTime;

        // 同样的，如果晚了，不保存
        if (recordUpdateTime < wsUpdateTime) {
            if (!jsonObject.containsKey("progress")) {
                LOGGER.info("current no record to save.");
                return;
            }
            for (String key : keys) {
                WebSocket.webSocket.get(key).caseContent = WebSocket.webSocket.get(keySave).caseContent;
                WebSocket.webSocket.get(key).updateRecordTime = 0L;
            }

            StringBuilder executors;
            if (StringUtils.isEmpty(dto.getExecutors())) {
                executors = new StringBuilder(user);
            } else {
                String executor = dto.getExecutors();
                executors = new StringBuilder(executor);
                String[] list = executor.split(SystemConstant.COMMA);
                if (!Arrays.asList(list).contains(user)) {
                    //无重复则添加，又重复不添加
                    if (list.length == 0) {
                        executors.append(user);
                    } else {
                        executors.append(SystemConstant.COMMA).append(user);
                    }
                }
            }
            ExecRecord recordUpdate = new ExecRecord();
            recordUpdate.setId(Long.valueOf(recordId));
            recordUpdate.setExecutors(executors.toString());
            recordUpdate.setModifier(user);
            recordUpdate.setGmtModified(new Date(System.currentTimeMillis()));
            recordUpdate.setCaseContent(jsonProgress.toJSONString());
            recordUpdate.setFailCount(failCount);
            recordUpdate.setBlockCount(blockCount);
            recordUpdate.setIgnoreCount(ignoreCount);
            recordUpdate.setPassCount(passCount);
            recordUpdate.setTotalCount(totalCount);
            recordUpdate.setSuccessCount(successCount);
            LOGGER.info("[Case Update]Save record exec recordId={}, content={}", recordId, recordUpdate.toString());
            recordService.modifyRecord(recordUpdate);
        } else {
            for (String key : keys) {
                WebSocket.webSocket.get(key).updateRecordTime = 0L;
            }
        }
    }

    /**
     * 打开用例/任务
     * @see #onOpen(String, String, String, String, Session)
     */
    private void open(String caseId, String recordId, String isCore) throws IOException {
        Long id = Long.valueOf(caseId);
        TestCase testCase = caseMapper.selectOne(id);
        String res = testCase.getCaseContent();
        if (StringUtils.isEmpty(res)) {
            throw new CaseServerException("用例内容为空", StatusCode.WS_UNKNOWN_ERROR);
        }

        switch (isCore) {
            case "0": {
                // 这里是打开case的情况
                singleSendMessage(session, res);
                break;
            }
            case "3": {
                // 这里是打开record的情况
                RecordWsDto dto = recordService.getWsRecord(Long.valueOf(recordId));

                String recordContent = dto.getCaseContent();
                JSONObject recordObj = new JSONObject();
                if (StringUtils.isEmpty(recordContent)) {
                    // 其实当前任务还没有任何执行记录
                    LOGGER.info("first create record.");
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

                    JSONObject caseContent = JSON.parseObject(res);
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
                    singleSendMessage(session, caseContent.toJSONString());
                } else {
                    // 如果是全部的，那么直接把testcase 给 merge过来
                    JSONObject caseContent = JSON.parseObject(res);
                    TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
                    singleSendMessage(session, caseContent.toJSONString());
                }
                break;
            }
        }
    }

    /**
     * 封装对齐函数
     */
    public static String fill(String key) {
        return stringFill(key, 8, '0', true);
    }

    public static String stringFill(String source, int fillLength, char fillChar, boolean isLeftFill) {
        if (source == null || source.length() >= fillLength) {
            return source;
        }

        StringBuilder result = new StringBuilder(fillLength);
        int len = fillLength - source.length();
        if (isLeftFill) {
            for (; len > 0; len--) {
                result.append(fillChar);
            }
            result.append(source);
        } else {
            result.append(source);
            for (; len > 0; len--) {
                result.append(fillChar);
            }
        }
        return result.toString();
    }

    /**
     * 获取当前websocket对应的用户
     */
    public String getUser() {
        return user;
    }

    /**
     * 获取一类用例/任务下的所有正在编辑的人
     */
    public static List<String> getEditingUser(String caseId, String recordId) {
        lock.lock();
        try {
            // 从Websocket.keys中获取所有正在编辑的用户的前缀！
            String prefix = buildSerial(caseId, recordId);
            // 复制当前瞬间的拷贝，不受原本对象的干扰
            Map<String, WebSocket> wsMap = new HashMap<>(WebSocket.webSocket);
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, WebSocket> entry : wsMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().startsWith(prefix)) {
                    names.add(entry.getValue().getUser());
                }
            }
            return names;
        } finally {
            lock.unlock();
        }
    }

}


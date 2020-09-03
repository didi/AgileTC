package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.EnvEnum;
import com.xiaoju.framework.entity.ExecRecord;
import com.xiaoju.framework.entity.PriorityEnv;
import com.xiaoju.framework.entity.TestCase;
import com.xiaoju.framework.service.ExecRecordService;
import com.xiaoju.framework.service.WebSocketService;
import com.xiaoju.framework.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Created by didi on 2019/9/23.
 */
@Slf4j
@Component
@ServerEndpoint(value = "/api/case/{caseId}/{recordId}/{isCore}/{user}")
public class WebSocket {

    public static WebSocketService webSocketService;
    public static ExecRecordService execRecordService;
    public static ConcurrentHashMap<String, Integer> userInfo = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, WebSocket> webSocket = new ConcurrentHashMap<>();
    private static List<String> keys = new ArrayList<>();

    private String caseId;
    private Session session;
    private String caseContent;
    private long updateCaseTime;
    private long updateRecordTime;
    private String recordId;
    private String isCore;
    private String user;
    private EnvEnum envEnum;
    private long pingTimeStamp;
    private long pongTimeStamp;

    public String toString() {
        return "caseId: " + caseId + ", session: " + session.getId() + ", recordId: " + recordId + ", isCore: " + isCore;
    }

    static {
        log.info("ping pong thread start.");
        ThreadPoolExecutor THREADPOOL = new ThreadPoolExecutor(1, 2, 3,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        THREADPOOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        for (Map.Entry<String, WebSocket> entry : webSocket.entrySet()) {
                            if (!entry.getValue().caseId.equals("undefined")) {
                                entry.getValue().sendMessage("ping ping ping");
                                if (System.currentTimeMillis() - entry.getValue().pongTimeStamp > 6000) {
                                    log.error("ping pong failed. prepare to close" + entry.getValue().toString());
                                    entry.getValue().onClose();
                                }
                            }
                        }
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    log.error("ping pong error. ", e);
                }
            }
        });
    }

    @OnOpen
    public synchronized void onOpen(@PathParam(value = "caseId") String caseId,
                                    @PathParam(value = "recordId") String recordId,
                                    @PathParam(value = "isCore") String isCore,
                                    @PathParam(value = "user") String user,
                                    Session session) {
        try {
            log.info("open channel params: " + caseId + " " + recordId + " " + isCore);
            this.session = session;
            this.caseId = stringFill(caseId, 8, '0', true);
            this.recordId = stringFill(recordId, 8, '0', true);
            this.isCore = isCore;
            this.user = user;
            this.updateCaseTime = 0;
            this.updateRecordTime = 0;
            this.pongTimeStamp = System.currentTimeMillis();
            log.info("link success, " + toString());
            //冒烟用例只需查看，无需更改，同步
            if (!isCore.equals("2") && !this.caseId.equals("undefined")) {
                keys.add(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true));
                webSocket.put(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true), this);
            }
            long count = webSocket.size();
            log.info("open case " + this.caseId + ", current local-total users: " + count);

            /* 打开当前主用例的用户数大于1后，会先更新case */
            if (getKeysByCaseId(this.caseId, this.recordId).size() > 1) {
                log.info("current users is more than 2, need to save first.");
                save(this.caseId, this.recordId, isCore, user);
            }

            open(this.caseId, this.recordId, isCore);
        } catch (Exception e) {
            log.info("open channel failed. \n" + toString());
            log.error("open channel failed. " + e);
            // TODO: 需要考虑回滚
        }
    }

    /**
     * 保存之前未保存的最新case/记录
     */
    public void updateLatestCase(String caseId, String recordId, String isCore, String user) {
        /* 多服务：打开当前主用例的用户数大于0，会先更新case */
        List<String> keyRet = getKeysByCaseId(caseId, recordId);
        if (keyRet.size() > 2) {
            log.info("openCase, save first." + caseId + ",recordId:" + recordId + ",isCore:" + isCore + ",user:" + user);
            save(caseId, recordId, isCore, user);
        }
    }

    @OnClose
    public synchronized void onClose() {
        try {
            log.info("onclose :" + this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true));

            if (!this.caseId.equals("undefined")) {
                save(this.caseId, this.recordId, this.isCore, this.user);
            }
            webSocket.remove(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true), this);
            WebSocket.keys.remove(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true));
            int users = WebSocket.keys.size();
            log.info("debug--- current case total users: " + users);

        } catch (Exception e) {
            log.error("close channel failed. " + e);
        }
    }

    @OnMessage(maxMessageSize = 1048576)
    public synchronized void onMessage(String message, Session session) {
        if (message.contains("pongpongpong")) {
            pongTimeStamp = System.currentTimeMillis();
            return;
        }

        if (null == webSocket.get(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true))) {
            sendMessage(session, StatusCode.HTTP_ACESS_ERROR.getCode());
            log.error("websocket already closed. caseid: " + this.caseId + ", recordid: " + this.recordId);
            return;
        }

        log.info("receive message: " + message);
        try {
            JSONObject request = JSON.parseObject(message);

            if (null == request.getString("patch")) {
                log.info("open case first");
                // TODO: 内容落库
                return;
            } else {
                JSONArray patch = (JSONArray) request.get("patch");
                long currentVersion = ((JSONObject) request.get("case")).getLong("base");
                String msg2Other = patch.toJSONString().replace("[[{", "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + String.valueOf(currentVersion + 1) + "},{");
                String msg2Own = "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + String.valueOf(currentVersion + 1) + "}]]";
                if (patch.toJSONString().contains("/progress")) {
                    log.info("current user is modifing record, send to others who open the same record");
                    sendMessage(getKeysByCaseId(this.caseId, this.recordId, stringFill(session.getId(), 8, '0', true)), msg2Other);
                    this.updateRecordTime = System.currentTimeMillis();
                } else {
                    log.info("current user is modifing case content, send to others who open this case.");
                    if (this.envEnum.equals(EnvEnum.TestQaEnv) || this.envEnum.equals(EnvEnum.TestRdEnv)) {
                        // do nothing
                    } else {
                        sendMessage(getKeysByCaseId(this.caseId, this.recordId, stringFill(session.getId(), 8, '0', true)), msg2Other);
                        this.updateCaseTime = System.currentTimeMillis();
                        log.info("current session: " + this.session + ". update time: " + this.updateCaseTime);
                    }
                }

                sendMessage(session, msg2Own);
                log.info("message to others: " + msg2Other);
                log.info("message to own: " + msg2Own);
                this.caseContent = ((JSONObject) request.get("case")).toJSONString().replace("\"base\":" + currentVersion, "\"base\":" + (currentVersion + 1));
                log.info("current case: " + this.caseContent.substring(0, 20));

            }
        } catch (Exception e) {
            log.error("receive exception. " + e);
        }

    }

    /**
     * onmessage同步本地消息内容
     *
     * @param patch
     * @param currentVersion
     * @param caseId
     * @param recordId
     * @param sessionId
     */
    public void sendMessageToOthers(JSONArray patch, long currentVersion, String caseId, String recordId, String sessionId) {
        log.info("receive diff, update message and send to others,caseId:" + caseId + ",recordId:" + recordId + ",sessionId" + sessionId);

        String msg2Other = patch.toJSONString().replace("[[{", "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + String.valueOf(currentVersion + 1) + "},{");

        sendMessage(getKeysByCaseId(caseId, recordId, stringFill(sessionId, 8, '0', true)), msg2Other);

        log.info("message to others: " + msg2Other);
    }

    @OnError
    public synchronized void onError(Session session, Throwable e) {
        log.error("onerror. " + e.getMessage());
        try {
            save(this.caseId, this.recordId, this.isCore, this.user);
            webSocket.remove(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true), this);
            long count = WebSocket.webSocket.size();
            log.info("error happend. close case:" + this.caseId + ", current local-total users: " + count);

            WebSocket.keys.remove(this.caseId + this.recordId + stringFill(session.getId(), 8, '0', true));
            int users = WebSocket.keys.size();

            log.info("error happend. current case total users: " + users);
        } catch (Exception excep) {
            log.error("error close channel failed. " + excep.getMessage());
        }
    }


    private void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
            if (!message.contains("ping ping ping")) {
                log.info("=> message to session [" + session + "] with msg: " + message);
            }
        } catch (Exception e) {
            log.error("send message to[" + this.caseId + "] with [" + message + "] failed.", e);
        }
    }

    private void sendMessage(Session s, String message) {
        try {
            s.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error("send message to session[" + session + "] with [" + message + "] failed.", e);
        }
    }

    /* 给指定用户发送消息 */
    private void sendMessage(String key, String message) {
        if (webSocket.get(key) != null) {
            sendMessage(webSocket.get(key).session, message);
        }
    }

    /* 批量发送消息 */
    private void sendMessage(List<String> keys, String message) {
        for (String key : keys) {
            sendMessage(key, message);
        }
    }


    //获取本地机器的
    /* 根据caseId获取当前打开该用例的所有用户或打开记录的用户 */
    private List<String> getKeysByCaseId(String caseId, String recordId) {
        List<String> keysRet = new ArrayList<>();

        for (String key : keys) {
            if (key.startsWith(caseId + recordId)) {// todo: 补齐位数
                keysRet.add(key);
            }
        }
        log.info("open local-current case's user. current keys1:" + keysRet.toString());
        return keysRet;
    }

    /* 根据caseId和session id获取当前打开该case的其他用户(非冒烟) */
//    private List<String> getKeysByCaseId(String caseId, String sid) {
//        List<String> keysRet = new ArrayList<>();
//
//        for (String key: keys) {
//            if (key.startsWith(caseId) && !key.endsWith(sid)
//                    && !(webSocket.get(key).envEnum.equals(EnvEnum.TestQaEnv) || webSocket.get(key).envEnum.equals(EnvEnum.TestRdEnv))) {
//                keysRet.add(key);
//            }
//        }
//        log.info("open current case's other user. current keys2:" + keysRet.toString());
//        return keysRet;
//    }

    /* 根据caseId和recordId, session id获取当前打开该case和recordid的其他用户，执行记录等信息需要同步过去 */
    private List<String> getKeysByCaseId(String caseId, String recordId, String sid) {
        List<String> keysRet = new ArrayList<>();

        for (String key : keys) {
            if (key.startsWith(caseId + recordId) && !key.equals(caseId + recordId + sid)) {
                keysRet.add(key);
            }
        }
        log.info("open current case and record's other user. current keys3:" + keysRet.toString());
        return keysRet;
    }

    /**
     * caseId: 用例ID
     * recordId: 执行记录ID
     * isCore: 是否冒烟用例
     */
    private void save(String caseId, String recordId, String isCore, String user) {
        //冒烟case不涉及保存
        if (isCore.equals("2"))
            return;
        if (!recordId.equals("undefined")) {//保存记录时，不保存用例内容
            saveRecord(caseId, recordId, user);
            return;
        }
        saveCase(caseId);
    }

    /*执行记录需要保留多份*/

    private void saveRecord(String caseId, String recordId, String user) {
        List<String> keys = getKeysByCaseId(caseId, recordId);
        long maxTime = 0;
        String keySave = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonProgress = new JSONObject();
        int totalCount = 0;
        int passCount = 0;
        int successCount = 0;
        // 将用例内容更新为最新
        for (String key : keys) {
            if (webSocket.get(key).updateRecordTime > maxTime) {
                maxTime = webSocket.get(key).updateRecordTime;
                jsonObject = TreeUtil.parse(webSocket.get(key).caseContent);
                jsonProgress = jsonObject.getJSONObject("progress");
                totalCount = jsonObject.getInteger("totalCount");
                passCount = jsonObject.getInteger("passCount");
                successCount = jsonObject.getInteger("successCount");
                keySave = key;
            }
        }

        log.info("caseId:" + caseId + ",recordId:" + recordId + ",user:" + user + ",sava record progress:" + jsonProgress);

        if (keySave.equals("")) {//无需更新
            return;
        }

        //获取数据库更新时间
        ExecRecord orgRecord = execRecordService.getRecordById(Long.parseLong(recordId));
        Long recordUpdateTime = orgRecord.getGmtModified().getTime();
        Long wsUpdateTime = webSocket.get(keySave).updateRecordTime;
        if (recordUpdateTime < wsUpdateTime) {
            if (!jsonObject.containsKey("progress")) {
                log.info("current no record to save.");
                return;
            } else {
                log.info("save record content . " + webSocket.get(keySave).caseContent);
            }
            for (String key : keys) {
                webSocket.get(key).caseContent = webSocket.get(keySave).caseContent;
                webSocket.get(key).updateRecordTime = 0L;
            }

            StringBuilder executors = new StringBuilder();

            if (orgRecord.getExecutors() == null || orgRecord.getExecutors().equals("")) {
                executors = new StringBuilder(user);
            } else {
                String executor = orgRecord.getExecutors();
                executors = new StringBuilder(executor);
                String[] list = executor.split(",");
                if (!Arrays.asList(list).contains(user)) {
                    //无重复则添加，又重复不添加
                    if (list.length == 0) {
                        executors.append(user);
                    } else {
                        executors.append("," + user);
                    }
                }
            }
            ExecRecord execRecord = new ExecRecord();
            execRecord.setId(Long.valueOf(recordId));
            execRecord.setCaseContent(jsonProgress.toJSONString());
            execRecord.setPassCount(passCount);
            execRecord.setTotalCount(totalCount);
            execRecord.setSuccessCount(successCount);
            //execRecord.setProgressRate(passCount*100/totalCount);
            execRecord.setExecutors(executors.toString());
            log.info("save " + keySave + " record. " + execRecord.toString() + "\nexecutor:" + executors.toString());
            execRecordService.modifyTestRecord(execRecord);

        } else {
            for (String key : keys) {
                webSocket.get(key).updateRecordTime = 0L;
            }
        }
    }


    /*case世纪只保留一份，非smk的其他case*/
    private void saveCase(String caseId) {
        TestCase testCase = new TestCase();
        testCase.setId(Long.valueOf(caseId));
        List<String> keys = getKeysByCaseId(caseId, "undefined");
        long maxTime = 0;
        String keySave = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonContent = new JSONObject();


        // 将用例内容更新为最新
        for (String key : keys) {
            //log.info("current case session: " + webSocket.get(key).session + ", content: " + webSocket.get(key).caseContent);
            if (webSocket.get(key).updateCaseTime > maxTime) {
                maxTime = webSocket.get(key).updateCaseTime;
                jsonObject = TreeUtil.parse(webSocket.get(key).caseContent);
                jsonContent = jsonObject.getJSONObject("content");
                keySave = key;
            }
        }

        if (keySave.equals("")) {//无需更新
            return;
        }

        //对比用例http更新时间和socket更新时间
        testCase = webSocketService.selectByPrimaryKey(Long.parseLong(caseId));
        Long tcUpdateTime = testCase.getGmtModified().getTime();
        Long wsTcUpdateTime = webSocket.get(keySave).updateCaseTime;
        //数据库更新时间大于socket最大更新时间则不需要保存
        if (tcUpdateTime >= wsTcUpdateTime) {
            log.info("caseid:" + caseId + "数据已是最新，no content to save.");
            for (String key : keys) {
                webSocket.get(key).caseContent = testCase.getCaseContent();
                webSocket.get(key).updateCaseTime = 0L;
            }
            return;
        } else {
            if (!jsonObject.containsKey("content")) {
                log.info("caseid:" + caseId + "current not content to save.");
                return;
            } else {

                /* 更新其他session的case content */
                for (String key : keys) {
                    webSocket.get(key).caseContent = webSocket.get(keySave).caseContent;
                    webSocket.get(key).updateCaseTime = 0L;
                }

                testCase.setCaseContent(jsonContent.toJSONString());
                Date now = new Date(wsTcUpdateTime);//更新时间为wsTcUpdateTime
                testCase.setGmtModified(now);
                log.info("caseid:" + caseId + " save content websocket. " + jsonContent.toJSONString());
                int ret = webSocketService.update(testCase);
                log.info("caseid:" + caseId + "save result: " + ret);
            }
        }
    }

    // 打开用例
    private void open(String caseId, String recordId, String isCore) {
        String res = webSocketService.selectCaseById(caseId);
        if (null == res || res.equals("")) { //
            log.error("database content is empty, need to see the reason.");
        }

        switch (isCore) {
            case "0": {// 原始用例打开场景
                this.envEnum = EnvEnum.SourceEnv;
                sendMessage(res); // 第一次发送内容，向当前客户端发送内容
                break;
            }
            case "1": { // 核心用例
                this.envEnum = EnvEnum.SourceEnv; // TODO: by xf
                sendMessage(res);
                break;
            }
            case "2": { // 冒烟用例
                this.envEnum = EnvEnum.SmkEnv;
                JSONObject caseContent = JSON.parseObject(res);

                JSONObject caseRoot = caseContent.getJSONObject("root");
                Stack<JSONObject> objCheck = new Stack<>();
                Stack<IntCount> iCheck = new Stack<>();
                objCheck.push(caseRoot);

                List<String> list = new ArrayList<>();
                list.add(PriorityEnv.Priority0.getValue().toString());
                //获取priority=1（p0）的数据
                TreeUtil.getPriority(objCheck, iCheck, caseRoot, list);

                log.info("get smk case." + caseContent.toJSONString().substring(0, 20));
                sendMessage(caseContent.toJSONString());
                break;
            }
            case "3": { // 场景用例
                ExecRecord execRecord = execRecordService.getRecordById(Long.valueOf(recordId));

                this.envEnum = EnvEnum.transfer(execRecord.getEnv());
                String record = execRecord.getCaseContent();
                JSONObject recordObj = new JSONObject();
                if (record == null || record.equals("")) {
                    log.info("first create record.");

                } else if (record.startsWith("[{")) {
                    JSONArray jsonArray = JSON.parseArray(record);
                    for (Object o : jsonArray) {
                        recordObj.put(((JSONObject) o).getString("id"), ((JSONObject) o).getLong("progress"));
                    }
                } else if (recordObj.containsKey("root")) {
                    log.warn("current is old record, need to parse first.");
                    recordObj = TreeUtil.parse(record).getJSONObject("progress");
                    execRecord.setCaseContent(recordObj.toJSONString());
                    execRecordService.modifyTestRecord(execRecord);
                } else {
                    log.info("normal record content.");
                    recordObj = JSON.parseObject(record);
                }

                IntCount ExecCount = new IntCount(recordObj.size());
                //有圈选条件-根据圈选条件取用例(有具体圈选条件)条件)
                if (execRecord.getChooseContent() != null && !execRecord.getChooseContent().equals("") && !execRecord.getChooseContent().contains("\"priority\":[\"0\"]")) {
                    String choose_content = execRecord.getChooseContent();//转换
                    Map<String, List<String>> chosen = JSON.parseObject(choose_content, Map.class);

                    JSONObject caseContent = JSON.parseObject(res);
                    JSONObject caseRoot = caseContent.getJSONObject("root");
                    Stack<JSONObject> objCheck = new Stack<>();

                    Stack<IntCount> iCheck = new Stack<>();
                    objCheck.push(caseRoot);

                    List<String> priority = chosen.get("priority");
                    List<String> resource = chosen.get("resource");
                    //获取对应级别用例
                    if (priority != null && priority.size() > 0)
                        TreeUtil.getPriority(objCheck, iCheck, caseRoot, priority);
                    if (resource != null && resource.size() > 0)
                        TreeUtil.getChosenCase(caseRoot, new HashSet<>(resource), "resource");

                    log.info("get priority " + priority.toString() + " case: " + caseContent.toJSONString());

                    try {
                        TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
                        log.info("get record " + caseContent.toJSONString().substring(0, 20));
                    } catch (Exception e) {
                        log.warn("there is no record. " + record);
                    }
                    log.info("未标优先级，但是根据优先级圈用例：" + caseContent.toJSONString());
                    sendMessage(caseContent.toJSONString());
                    break;

                } else if (execRecord.getEnv() != null) {//有环境id时根据记录的环境表示取用例
                    switch (execRecord.getEnv()) {
                        case 0:
                        case 1:
                        case 2: {
                            JSONObject caseContent = JSON.parseObject(res);
                            try {
                                TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
                                log.info("get record. " + caseContent.toJSONString().substring(0, 20));
                            } catch (Exception e) {
                                log.error("there is no record." + record);
                            }
                            sendMessage(caseContent.toJSONString());
                            break;
                        }
                        case 3:
                        case 4: {
                            JSONObject caseContent = JSON.parseObject(res);
                            JSONObject caseRoot = caseContent.getJSONObject("root");
                            Stack<JSONObject> objCheck = new Stack<>();

                            Stack<IntCount> iCheck = new Stack<>();
                            objCheck.push(caseRoot);

                            List<String> list = new ArrayList<>();
                            list.add(PriorityEnv.Priority0.getValue().toString());
                            //获取p0用例
                            TreeUtil.getPriority(objCheck, iCheck, caseRoot, list);

                            log.info("get smk case: " + caseContent.toJSONString());
                            try {
                                TreeUtil.mergeExecRecord(caseContent.getJSONObject("root"), recordObj, ExecCount);
                                log.info("get smk record " + caseContent.toJSONString().substring(0, 20));
                            } catch (Exception e) {
                                log.warn("there is no record. " + record);
                            }
                            sendMessage(caseContent.toJSONString());
                            break;
                        }
                        default: {
                            log.error("env param error.");
                            break;
                        }
                    }
                    break;
                } else {
                    log.error("该记录无圈选条件、执行环境!请检查");
                    break;
                }

            }
            default: {
                log.error("iscore param error.");
                break;
            }
        }

    }

    private static String stringFill(String source, int fillLength, char fillChar, boolean isLeftFill) {
        if (source == null || source.length() >= fillLength) return source;

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


    //http接口更新用例或记录时同时更新socket里的内容
    public void updateSocketContent(String caseId, String recordId, String content) {
        caseId = stringFill(caseId, 8, '0', true);
        recordId = stringFill(recordId, 8, '0', true);
        List<String> keys = getKeysByCaseId(caseId, recordId);
        log.info("update socket content after update database data .caseId:" + caseId + "reocordId:" + recordId);
        // 将用例内容更新为最新
        for (String key : keys) {
            webSocket.get(key).caseContent = content;
            webSocket.get(key).updateCaseTime = 0L;
        }
    }

}


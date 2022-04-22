package com.xiaoju.framework.handler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.xiaoju.framework.entity.persistent.ExecRecord;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.entity.xmind.IntCount;
import com.xiaoju.framework.mapper.ExecRecordMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.util.BitBaseUtil;
import com.xiaoju.framework.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service(value = "socketIOService")
public class SocketIOServiceImpl implements SocketIOService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIOServiceImpl.class);

    @Autowired
    private SocketIOServer socketIOServer;
    private final ReentrantLock globalLock = new ReentrantLock(); // 全局锁

    private static Map<String, ReentrantLock> roomLockMap = new ConcurrentHashMap<>(); // 用于防止并发

    // 用来存已连接的客户端
    private static Map<String, Set<SocketIOClient>> roomClientMap = new ConcurrentHashMap<>();
    private static Map<String, TestCase> roomContentMap = new ConcurrentHashMap<>();
    private static Map<String, ExecRecord> roomRecordMap = new ConcurrentHashMap<>();
    private static Map<String, SocketIOClient> roomLocker = new ConcurrentHashMap<>(); // 用于外部用户锁页面可编辑的功能
    private static Map<SocketIOClient, Integer> clientNumMap = new ConcurrentHashMap<>();

    static ConcurrentHashMap<SocketIOClient, RoomEntity> clientRoomMap = new ConcurrentHashMap<>();

    @Resource
    private ExecRecordMapper recordMapper;
    @Resource
    private TestCaseMapper caseMapper;

    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    // 消息处理
    private ExecutorService executorIngressService = Executors.newFixedThreadPool(3);

    // 消息转发
    private ExecutorService executorEgressService = Executors.newFixedThreadPool(1);

    /**
     * Spring IoC容器创建之后，在加载SocketIOServiceImpl Bean之后启动
     * @throws Exception
     */
    @PostConstruct
    private void autoStartup() throws Exception {
        start();
    }

    /**
     * Spring IoC容器在销毁SocketIOServiceImpl Bean之前关闭,避免重启项目服务端口占用问题
     * @throws Exception
     */
    @PreDestroy
    private void autoStop() throws Exception  {
        stop();
    }

    @OnConnect
    public void connect() {
        LOGGER.info("socket server connect.");
    }
    @Override
    public void start() {
        // 监听客户端连接
        socketIOServer.addConnectListener(client -> {
            ClientEntity clientEntity = getRoomFromClient(client);
            String roomId = clientEntity.getRoomId();
            RoomEntity roomEntity = RoomFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper);
            roomEntity.addClient(client);
            clientRoomMap.put(client, roomEntity);
            LOGGER.info("connected caseId: " + clientEntity.getCaseIdStr() + ", roomId: " + roomId);

            client.joinRoom(roomId);

            executorEgressService.submit(new AckEgressTask("open_event",
                    PushMessage.builder().message(roomEntity.getCaseContent()).build(), client));

            executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                    PushMessage.builder().message(roomEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));
//
//            globalLock.lock();
//
//            RoomEntity roomEntity = getRoomFromClient(client);
//            String roomId = roomEntity.getRoomId();
//
//            LOGGER.info("connected caseId: " + roomEntity.getCaseIdStr() + ", roomId: " + roomId);
//
//            roomLockMap.put(roomId, new ReentrantLock());
//            roomLockMap.get(roomId).lock();
//            if (clientNumMap.containsKey(client)) {
//                Integer num = clientNumMap.get(client);
//                clientNumMap.replace(client, num+1);
//
//            } else {
//                clientNumMap.put(client, 1);
//            }
//
//            globalLock.unlock();
//
//            client.joinRoom(roomId);
//
//            Set<SocketIOClient> clients;
//            if (roomClientMap.containsKey(roomId)) { // todo: 此处还会有时序问题，需要加锁
//                clients = roomClientMap.get(roomId);
//                clients.add(client);
//                LOGGER.info("room clients number:" + clients.size());
//            } else {
//                clients = new HashSet<>();
//                clients.add(client);
//                roomClientMap.put(roomId, new HashSet<SocketIOClient>(){{add(client);}});
//            }
//
//            if (roomContentMap.containsKey(roomId)) { // todo: 此处还会有时序问题，需要加锁
//                // do nothing
//            } else {
//                TestCase testCase = caseMapper.selectOne(roomEntity.getCaseId());
//                String res = testCase.getCaseContent();
//                if (org.springframework.util.StringUtils.isEmpty(res)) {
//                    LOGGER.error(Thread.currentThread().getName() + ": 用例内容为空");
//                    // todo: 此处需要走异常处理流程
//                } else {
//                    LOGGER.info("case content is: " + testCase.getCaseContent());
//                }
//
//                if (roomEntity.getRecordId() != null) {
//                    String recordContent = mergeRecord(roomEntity.getRecordId(), testCase.getCaseContent());
//                    testCase.setCaseContent(recordContent);
////                    testCase.setGroupId(roomEntity.getRecordId());
//                }
//
//                roomContentMap.put(roomId, testCase);
//            }
//
//            roomLockMap.get(roomId).unlock();
//            client.sendEvent("open_event", PushMessage.builder().message(roomContentMap.get(roomId).getCaseContent()).build());
//            BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);
//            List<String> client_names = new ArrayList<>();
//            for (SocketIOClient c: clients) {
//                client_names.add(c.getHandshakeData().getSingleUrlParam("user"));
//            }
//
//            broadcastOperations.sendEvent("connect_notify_event", PushMessage.builder().message(String.join(",", client_names)).build());
        });

        // 监听客户端断开连接
        socketIOServer.addDisconnectListener(client -> {

//            globalLock.lock();
            ClientEntity clientEntity = getRoomFromClient(client);
            String roomId = clientEntity.getRoomId();
            RoomEntity roomEntity = RoomFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper);
            roomEntity.removeClient(client);
            clientRoomMap.remove(client);
            executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                    PushMessage.builder().message(roomEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));
            client.leaveRoom(roomId);
            client.disconnect();

//            String user = client.getHandshakeData().getSingleUrlParam("user");
//            Integer num = clientNumMap.get(client);
//            if (num.equals(1)) {
//                clientNumMap.remove(client);
//                client.leaveRoom(roomId);
//            } else if (num > 1) {
//                clientNumMap.replace(client, num-1);
//                globalLock.unlock();
//                LOGGER.info(user + " opened " + num + " sessions, 1 disconnect, roomid: " + roomId);
//                return;
//            } else {
//                LOGGER.error("client disconnect error. client num map:" + clientNumMap.get(client));
//                globalLock.unlock();
//                return;
//            }
//            roomLockMap.get(roomId).lock();
//            globalLock.unlock();
//
//            LOGGER.info(user + " disconnect, roomid: " + roomId);
//
//            Set<SocketIOClient> clients = roomClientMap.get(roomId);
//            clients.remove(client);
//            if (clients.size() == 0) {
//
//                TestCase testCaseToUpdate = roomContentMap.get(roomId);
//                String testContent = testCaseToUpdate.getCaseContent();
//
//                try {
//                    if (roomEntity.getRecordId() == null) {
//                        // 保存用例 & 清空map
//
//                        TestCase testCaseBase = caseMapper.selectOne(roomEntity.getCaseId());
//
//                        JsonNode saveContent = jsonMapper.readTree(testContent);
//                        JsonNode baseContent = jsonMapper.readTree(testCaseBase.getCaseContent());
//                        if (saveContent.get("base").asInt() > baseContent.get("base").asInt()) {
//                            // 保存落库
//                            TreeUtil.caseDFSValidate(saveContent.get("root"));
//                            testCaseToUpdate.setCaseContent(saveContent.toString());
//                            testCaseToUpdate.setGmtModified(new Date(System.currentTimeMillis()));
//                            testCaseToUpdate.setModifier(user);
//                            int ret = caseMapper.update(testCaseToUpdate);
//                            if (ret < 1) {
//                                LOGGER.error(Thread.currentThread().getName() + ": 数据库用例内容更新失败。 ret = " + ret);
//                                LOGGER.error("应该保存的用例内容是：" + testContent);
//                            }
//                        } else {
//                            // 不保存
//                            LOGGER.info(Thread.currentThread().getName() + "不落库." + testContent);
//                        }
//                    } else { // record保存逻辑
////                        testCase.setGmtModified(new Date(System.currentTimeMillis()));
//                        ExecRecord record = recordMapper.selectOne(roomEntity.getRecordId());
//                        if (record == null) {
//                            throw new CaseServerException("执行任务不存在", StatusCode.NOT_FOUND_ENTITY);
//                        }
//                        JSONObject jsonObject = TreeUtil.parse(testContent);
//                        JSONObject jsonProgress = jsonObject.getJSONObject("progress");
//                        Integer totalCount = jsonObject.getInteger("totalCount");
//                        Integer passCount = jsonObject.getInteger("passCount");
//                        Integer failCount = jsonObject.getInteger("failCount");
//                        Integer blockCount = jsonObject.getInteger("blockCount");
//                        Integer successCount = jsonObject.getInteger("successCount");
//                        Integer ignoreCount = jsonObject.getInteger("ignoreCount");
//
//                        List<String> names = Arrays.stream(record.getExecutors().split(COMMA)).filter(e->!StringUtils.isEmpty(e)).collect(Collectors.toList());
//                        long count = names.stream().filter(e -> e.equals(user)).count();
//
//                        if (count > 0) {
//                            // 有重合，不管了
//                            ;
//                        } else {
//                            // 没重合往后面塞一个
//                            names.add(user);
//                        }
//
//                        record.setExecutors(String.join(",", names));
//                        record.setModifier(user);
//                        record.setGmtModified(new Date(System.currentTimeMillis()));
//                        record.setCaseContent(jsonProgress.toJSONString());
//                        record.setFailCount(failCount);
//                        record.setBlockCount(blockCount);
//                        record.setIgnoreCount(ignoreCount);
//                        record.setPassCount(passCount);
//                        record.setTotalCount(totalCount);
//                        record.setSuccessCount(successCount);
//                        recordMapper.update(record);
//
//                        LOGGER.info(Thread.currentThread().getName() + ": 数据库用例记录更新。record: " + record.getCaseContent());
//                    }
//
//                    // 内容清空
//                    roomLockMap.get(roomId).unlock();
//                    roomLockMap.remove(roomId);
//                    roomContentMap.remove(roomId);
//                    roomClientMap.remove(roomId);
//
//                    if (roomLocker.containsKey(roomId)) roomLocker.remove(roomId);
//
////                    client.sendEvent("reconnect");
//                } catch (Exception e) {
//                    LOGGER.error("json parse error. ", e);
//                    roomLockMap.get(roomId).unlock();
//                }
//            } else {
//                BroadcastOperations broadcastOperations = socketIOServer.getRoomOperations(roomId);
//                List<String> client_names = new ArrayList<>();
//                for (SocketIOClient c: clients) {
//                    client_names.add(c.getHandshakeData().getSingleUrlParam("user"));
//                }
//
//                broadcastOperations.sendEvent("connect_notify_event", PushMessage.builder().message(String.join(",", client_names)).build());
//                roomLockMap.get(roomId).unlock();
//            }
//            client.disconnect();

        });

        // 处理自定义的事件，与连接监听类似,event为事件名，PushMessage为参数实体类　　　　 // 监听前端发送的事件
        socketIOServer.addEventListener("edit", EditMessage.class, (client, data, ackSender) -> {
            executorIngressService.submit(new EditIngressTask(client, socketIOServer, clientRoomMap.get(client), executorEgressService, data));
        });

        socketIOServer.addEventListener("case_design_event", CaseDesignMessage.class, (client, data, ackSender) -> {
            executorIngressService.submit(new CaseDesignIngressTask(client, socketIOServer, clientRoomMap.get(client), executorEgressService, data));
        });

        socketIOServer.addEventListener("lock", PushMessage.class, (client, data, ackSender) -> {
            executorIngressService.submit(new LockIngressTask(client, socketIOServer, clientRoomMap.get(client), executorEgressService, data));
        });

        socketIOServer.start();

        LOGGER.info("socket server start.");
    }

    @Override
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            socketIOServer = null;
        }
    }


    private ClientEntity getRoomFromClient(SocketIOClient client) {
        ClientEntity clientEntity = new ClientEntity();
        String caseId = client.getHandshakeData().getSingleUrlParam("caseId");
        String recordId = client.getHandshakeData().getSingleUrlParam("recordId");
        clientEntity.setCaseIdStr(caseId);
        clientEntity.setCaseId(Long.valueOf(caseId));
        if (recordId != null) {
            clientEntity.setRecordId(Long.valueOf(recordId));
            clientEntity.setRecordIdStr(recordId);
            clientEntity.setRoomId(String.valueOf(BitBaseUtil.mergeLong(clientEntity.getRecordId(), clientEntity.getCaseId())));
        } else {
            clientEntity.setRoomId(caseId);
        }
        return clientEntity;
    }


}

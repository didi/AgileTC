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
import com.xiaoju.framework.mapper.CaseBackupMapper;
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
    static ConcurrentHashMap<SocketIOClient, RecordEntity> clientRecordRoomMap = new ConcurrentHashMap<>();

    @Resource
    private ExecRecordMapper recordMapper;
    @Resource
    private TestCaseMapper caseMapper;
    @Resource
    private CaseBackupMapper caseBackupMapper;

    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    // 消息处理
    private ExecutorService executorIngressService = Executors.newFixedThreadPool(2);

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
            if (null == clientEntity.getRecordId()) {
                RoomEntity roomEntity = RoomFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper);
                roomEntity.addClient(client);
                clientRoomMap.put(client, roomEntity);
                client.joinRoom(roomId);
                LOGGER.info("add case client room map size: " + clientRoomMap.keySet().size());
                executorEgressService.submit(new AckEgressTask("open_event",
                        PushMessage.builder().message(roomEntity.getCaseContent()).build(), client));

                executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                        PushMessage.builder().message(roomEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));

                LOGGER.info("connected caseId: " + clientEntity.getCaseIdStr() + ", roomId: " + roomId);

            } else {
                RecordEntity recordEntity = RecordFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper, clientEntity.getRecordId(), recordMapper);
                recordEntity.addClient(client);
                clientRoomMap.put(client, recordEntity);
                client.joinRoom(roomId);
                LOGGER.info("add record client room map size: " + clientRoomMap.keySet().size());
                executorEgressService.submit(new AckEgressTask("open_event",
                        PushMessage.builder().message(recordEntity.getCaseContent()).build(), client));

                executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                        PushMessage.builder().message(recordEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));

                LOGGER.info("connected caseId: " + clientEntity.getCaseIdStr() + ", recordid: " + clientEntity.getRecordId());
            }

//            client.joinRoom(roomId);

        });

        // 监听客户端断开连接
        socketIOServer.addDisconnectListener(client -> {

            ClientEntity clientEntity = getRoomFromClient(client);
            String roomId = clientEntity.getRoomId();
            if (null == clientEntity.getRecordId()) {
                RoomEntity roomEntity = RoomFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper);
                roomEntity.removeClient(client);
                if (roomEntity.getClientNum() == 0) {
                    RoomFactory.clearRoom(roomId);
                }
                clientRoomMap.remove(client);
                LOGGER.info("remove case client room map size: " + clientRoomMap.keySet().size());
                client.leaveRoom(roomId);
                executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                        PushMessage.builder().message(roomEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));
            } else {
                RecordEntity recordEntity = RecordFactory.getRoom(roomId, clientEntity.getCaseId(), caseMapper, clientEntity.getRecordId(), recordMapper);
                recordEntity.removeClient(client);
                if (recordEntity.getClientNum() == 0) {
                    RecordFactory.clearRoom(roomId);
                }
                clientRoomMap.remove(client);
                LOGGER.info("remove record client room map size: " + clientRoomMap.keySet().size());
                client.leaveRoom(roomId);
                executorEgressService.submit(new NotifyEgressTask("connect_notify_event",
                        PushMessage.builder().message(recordEntity.getClientName()).build(), socketIOServer.getRoomOperations(roomId)));
            }

            client.disconnect();

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

        socketIOServer.addEventListener("save", EditMessage.class, (client, data, ackSender) -> {
            executorIngressService.submit(new SaveIngressTask(client, socketIOServer, clientRoomMap.get(client), executorEgressService, data, caseBackupMapper));
        });

        socketIOServer.addEventListener("record_clear", EditMessage.class, (client, data, ackSender) -> {
            executorIngressService.submit(new RecordClearIngressTask(client, socketIOServer, clientRoomMap.get(client), executorEgressService));
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
        if (!recordId.equals("undefined")) {
            clientEntity.setRecordId(Long.valueOf(recordId));
            clientEntity.setRecordIdStr(recordId);
            clientEntity.setRoomId(String.valueOf(BitBaseUtil.mergeLong(clientEntity.getRecordId(), clientEntity.getCaseId())));
        } else {
            clientEntity.setRoomId(caseId);
        }
        return clientEntity;
    }

}

package com.xiaoju.framework.handler;


import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;

import com.xiaoju.framework.util.BitBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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

//    /**
//     * 依赖
//     * @see ApplicationConfig#setWebsocketService(com.xiaoju.framework.service.RecordService, com.xiaoju.framework.mapper.TestCaseMapper)
//     */
//    public static RecordService recordService;
//    public static TestCaseMapper caseMapper;
//
    /**
     * 每个websocket所持有的基本信息
     */
    private String caseId;
    private Session session;
    private String recordId;
    private String isCore;

    @Override
    public String toString() {
        return String.format("[Websocket Info][%s]caseId=%s, sessionId=%s, recordId=%s, isCoreCase=%s",
                recordId == null || CaseWsMessages.UNDEFINED.getMsg().equals(recordId) ? "测试用例" : "执行任务", caseId, session.getId(), recordId, isCore
        );
    }


    private static volatile Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private static final Object roomLock = new Object();

    static {
        Thread t = new Thread(() -> {
            while(true) {
                for (Room room : rooms.values()) {
                    room.broadcastRoomMessage(CaseMessageType.PING, CaseWsMessages.PING.getMsg());
                }
                try {
                    Thread.sleep(15000);
                } catch (Exception e) {
                    LOGGER.error("ping thread sleep error.", e);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
    public static Object getRoomLock() {
        return roomLock;
    }
    public static Map<Long, Room> getRooms() {
        return rooms;
    }
    public static Room getRoom(boolean create, long id) {
        if (create) {
            // todo: 清除的逻辑放到线程定时任务中
            if (rooms.get(id) == null) {
                synchronized (roomLock) { // TODO: 2021/9/2 此处使用的全局锁,可以优化为局部锁
                    if (rooms.get(id) == null) {
                        if (id > Integer.MAX_VALUE) { // 查看id的大小，如果大于最大值，则表示此id是record，room将recordroom加入
                            rooms.put(id, new RecordRoom(id));
                        } else {
                            rooms.put(id, new CaseRoom(id)); // 否则将caseRoom加入到rooms
                        }
                        LOGGER.info(Thread.currentThread().getName() + ": 新建Room成功，caseid=" + BitBaseUtil.getLow32(id) + ", record id: " + BitBaseUtil.getHigh32(id));
                    }
                }
            }
        }
        return rooms.get(id);
    }
    private Room.Player player;

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


        // 连基本的任务都不是，直接报错
        if (CaseWsMessages.UNDEFINED.getMsg().equals(caseId)) {
            throw new CaseServerException("用例id为空", StatusCode.WS_UNKNOWN_ERROR);
        }

        LOGGER.info(Thread.currentThread().getName() + ": [websocket-onOpen 开启新的session][{}]", toString());
        // 开启新的session，下面是创建新的client，record以及room
        final Client client = new Client(session, recordId, user);
        long record = recordId.equals(CaseWsMessages.UNDEFINED.getMsg()) ? 0l : Long.valueOf(recordId);
        final Room room = getRoom(true, BitBaseUtil.mergeLong(record, Long.valueOf(caseId)));

        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        player = room.createAndAddPlayer(client); // 给room添加一个新的client
                        if (room.getLock()) { // 如果room被锁住了
                            player.sendRoomMessageAsync("2lock"); // 异步的发送lock的消息
                        }
                        LOGGER.info(Thread.currentThread().getName() + ": player " + client.getClientName() + " 加入: " + player);
                    } catch (IllegalStateException e) {
                        client.sendMessage(CaseMessageType.NOTIFY, new String("0" + e.getMessage()));
                        client.close();
                    }
                } catch (RuntimeException e) {
                    LOGGER.error(Thread.currentThread().getName() + ": Unexcepted exception. e: " + e.getMessage());
                }
            }
        });
    }

    @OnClose
    public void onClose(@PathParam(value = "caseId") String caseId, @PathParam(value = "recordId") String recordId) {
        if (CaseWsMessages.UNDEFINED.getMsg().equals(caseId)) {
            throw new CaseServerException("用例id为空", StatusCode.WS_UNKNOWN_ERROR);
        }
        long record = recordId.equals(CaseWsMessages.UNDEFINED.getMsg()) ? 0l : Long.valueOf(recordId);
        long id = BitBaseUtil.mergeLong(record, Long.valueOf(caseId));
        final Room room = getRoom(false, id);
//        if (room.players.size() == 1) {
//            synchronized (roomLock) {
//                rooms.remove(Long.valueOf(id));
//                LOGGER.info(Thread.currentThread().getName() + ": [websocket-onClose 关闭当前Room成功]当前sessionid:" + room.players.get(0).getClient().getSession().getId());
//            }
//        }
        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    if (player != null) {
                        // 锁住的人离开，则给其他人发送解锁消息
                        if (player.getRoom().getLock() && player.getRoom().getLocker().equals(player.getClient().getSession().getId())) {
                            player.getRoom().unlock();
                            player.handleCtrlMessage("x" + "|" + "unlock");
                        }

                        player.removeFromRoom();

                        player = null;
                    }
                } catch (RuntimeException e) {
                    LOGGER.error(Thread.currentThread().getName() + ": 异常 " + e.toString(), e);
                }
            }
        });
    }

    @OnMessage(maxMessageSize = 1048576)
    public void onMessage(@PathParam(value = "caseId") String caseId, @PathParam(value = "recordId") String recordId,
                          String message, Session session) throws IOException {
        long record = recordId.equals(CaseWsMessages.UNDEFINED.getMsg()) ? 0l : Long.valueOf(recordId);
        final Room room = getRoom(false, BitBaseUtil.mergeLong(record, Long.valueOf(caseId)));
        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean dontSwallowException = false;
                    try {
                        char messageType = message.charAt(0);
                        String messageContent = message.substring(1);
                        switch (messageType) {
                            case '0': // 处理ping/pong消息，这个是一直在进行的
                                if (messageContent.equals(CaseWsMessages.PING.getMsg())) { // 如果messageContent是ping
                                    room.cs.get(session).sendMessage(CaseMessageType.PING, CaseWsMessages.PONG.getMsg());
                                } else if (messageContent.equals(CaseWsMessages.PONG.getMsg())) { // 如果messageContent是pong
                                    player.clearPingCount();
                                } else {
                                    LOGGER.error(Thread.currentThread().getName() + "ping pong 信息有误。消息是：" + message);
                                }
                                break;

                            case '1': // 处理编辑消息，包括对信息的编辑以及撤销等操作
                                if (player != null) {
                                    //todo：此处分隔符待优化
                                    LOGGER.info(Thread.currentThread().getName() + ": 消息内部处理中...");
                                    // todo: 此处msgid写死，未运用，msgid用于前端的重传逻辑。后续补充能力
                                    player.handleMessage(session.getId() + "|" + messageContent, 0);
                                }
                                break;

                            case '2': // 处理控制消息，进行对case加锁
                                LOGGER.info(Thread.currentThread().getName() + ": 收到控制消息... onMessage: " + message.trim());

                                if (messageContent.equals("lock")) { // lock消息
                                    if (player.getRoom().getLock()) {
                                        player.sendRoomMessageSync(CaseMessageType.NOTIFY,"2" + "failed" + "已经被人锁住了"); // 当前已经lock了,后续可以发送详细锁住人信息
                                        return;
                                    } else {
                                        player.getRoom().lock();
                                        player.getRoom().setLocker(session.getId());
                                    }
                                } else if(messageContent.equals("unlock")) {
                                    if (!player.getRoom().getLock()) { // 当前已经unlock状态
                                        player.sendRoomMessageSync(CaseMessageType.NOTIFY,"2" + "failed" + " 当前已经是解锁状态");
                                        return;
                                    } else {
                                        if (player.getRoom().getLocker().equals(session.getId())) { // 自己锁的
                                            player.getRoom().unlock();
                                            player.getRoom().setLocker("");
                                        } else {// 其他人锁的
                                            player.sendRoomMessageSync(CaseMessageType.NOTIFY,"2" + "failed" + "当前被其他人锁住了");
                                            return;
                                        }
                                    }
                                }
                                if (player != null) {
                                    player.handleCtrlMessage(session.getId() + "|" + messageContent);
                                }
                                break;
                            default:
                                LOGGER.error(Thread.currentThread().getName() + ": 这个消息类型不符合预期：" + message.trim());
                                break;
                        }

                    } catch (RuntimeException e) {
                        // 客户端发送的无效信息
                        // TODO: 可能要关闭连接
                        LOGGER.warn(Thread.currentThread().getName() + ": 运行异常: " + e.getMessage(), e);
                        if (dontSwallowException) {
                            throw e;
                        }
                    }

                } catch (RuntimeException e) {
                    LOGGER.error(Thread.currentThread().getName() + ": 异常: " + e.toString(), e);
                }
            }
        });

    }

    @OnError
    public void onError(Session session, Throwable e) {
        LOGGER.info(Thread.currentThread().getName() + ": [websocket-onError 会话出现异常]当前session={}, 原因={}", session.getId(), e);
        int count = 0;
        Throwable root = e;
        while (root.getCause() != null && count < 20) {
            root = root.getCause();
            count++;
        }
        if (root instanceof EOFException) {

        } else if (!session.isOpen() && root instanceof IOException) {

        } else {
            LOGGER.error(Thread.currentThread().getName() + ": [websocket-onError 会话出现异常]" + e.toString(), e);
        }
    }



}


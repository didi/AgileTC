package com.xiaoju.framework.handler;


import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;

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

    public static Room getRoom(boolean create, long id) {
//        Long cid = Long.valueOf(caseId);
        if (create) {
            // todo: 清除的逻辑放到线程定时任务中
            if (rooms.get(id) == null) {
                synchronized (roomLock) {
                    if (rooms.get(id) == null) {
                        if (id > Integer.MAX_VALUE) {
                            rooms.put(id, new RecordRoom(id));
                        } else {
                            rooms.put(id, new CaseRoom(id));
                        }
                        LOGGER.info(Thread.currentThread().getName() + ": 新建Room成功，caseid=" + (id&0xffffffffl) + ", record id: " + (id>>32));
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
        final Client client = new Client(session, recordId, user);
        long record = recordId.equals(CaseWsMessages.UNDEFINED.getMsg()) ? 0l : Long.valueOf(recordId);
        final Room room = getRoom(true, record << 32 | Integer.valueOf(caseId));

        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        player = room.createAndAddPlayer(client);
                        LOGGER.info(Thread.currentThread().getName() + ": player " + client.getClientName() + " 加入: " + player);
                    } catch (IllegalStateException e) {
                        client.sendMessage(new String("0" + e.getMessage()));
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
        long id = record << 32 | Integer.valueOf(caseId);
        final Room room = getRoom(false, id);
        if (room.players.size() == 1) {
            synchronized (roomLock) {
                rooms.remove(Long.valueOf(id));
                LOGGER.info(Thread.currentThread().getName() + ": [websocket-onClose 关闭当前session成功]当前sessionid:" + room.players.get(0).getClient().getSession().getId());
            }
        }
        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    if (player != null) {
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
        final Room room = getRoom(false, record << 32 | Integer.valueOf(caseId));
//        final Room room = getRoom(false, Long.valueOf(recordId) << 32 | Integer.valueOf(caseId));
        room.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean dontSwallowException = false;
                    try {
                        if (CaseWsMessages.PING.getMsg().equals(message)) {
                            room.cs.get(session).sendMessage(CaseWsMessages.PONG.getMsg());
                            return;
                        }

                        LOGGER.info(Thread.currentThread().getName() + ": 收到消息... onMessage: " + message.trim());
                        if (player != null) {
                            //todo：此处分隔符待优化
                            LOGGER.info(Thread.currentThread().getName() + ": 消息内部处理中...");
                            // todo: 此处msgid写死，未运用。
                            player.handleMessage(session.getId() + "|" + message, 2);
                        }
                    } catch (RuntimeException e) {
                        // Client sent invalid data.
                        // Ignore, TODO: maybe close connection
                        LOGGER.warn(Thread.currentThread().getName() + ": runtime exception: " + e.getMessage(), e);
                        if (dontSwallowException) {
                            throw e;
                        }
                    }

                } catch (RuntimeException e) {
                    LOGGER.error(Thread.currentThread().getName() + ": Unexpected exception: " + e.toString(), e);
                }
            }
        });

    }

    @OnError
    public void onError(Session session, Throwable e) {
        LOGGER.info(Thread.currentThread().getName() + ": [websocket-onError 会话出现异常]当前session={}, 原因={}", session.getId(), e.getMessage());
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


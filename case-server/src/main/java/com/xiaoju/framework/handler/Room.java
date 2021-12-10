package com.xiaoju.framework.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.service.RecordService;
import com.xiaoju.framework.util.BitBaseUtil;
import org.apache.poi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.flipkart.zjsonpatch.DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE;
import static com.flipkart.zjsonpatch.DiffFlags.OMIT_MOVE_OPERATION;

/**
 * Created by didi on 2021/3/22.
 */
public abstract class Room {
    private static final Logger LOGGER = LoggerFactory.getLogger(Room.class);

    private final ReentrantLock roomLock = new ReentrantLock();
    private volatile boolean closed = false;

    private static final boolean BUFFER_MESSAGES = true;
    private final Timer messageBroadcastTimer = new Timer();
    private volatile boolean locked = false;
    private volatile String locker = "";
    private static final int TIMER_DELAY = 30;
    private TimerTask activeBroadcastTimerTask;

    public static Boolean editInfoSaveToDB = true; // true:编辑信息保存到数据库; false:编辑信息保存到日志
    public static Long roomId;
    private static final int MAX_PLAYER_COUNT = 10;
    public final List<Player> players = new ArrayList<>();

    public final List<String> undoDiffs = new LinkedList<>();
    public final List<String> redoDiffs = new LinkedList<>();
    private Integer undoPosition;
    private Integer redoPosition;

    protected Integer lastUndoCounts;

    public final Map<Session, Client> cs = new ConcurrentHashMap<>();

    public static TestCaseMapper caseMapper;
    public static RecordService recordService;
    public static CaseBackupService caseBackupService;

    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    protected String testCaseContent;
    protected TestCase testCase;

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public boolean getLock() {
        return this.locked;
    }
    public String getLocker() {
        return this.locker;
    }
    public void setLocker(String locker) {
        this.locker = locker;
    }
    // id 前面部分是case id；后面部分是record id
    public Room(Long id) {
        this.roomId = id;
        long caseId = BitBaseUtil.getLow32(id);
        if (testCase != null) {
            return;
        }
        testCase = caseMapper.selectOne(caseId);
        String res = testCase.getCaseContent();
        if (StringUtils.isEmpty(res)) {
            LOGGER.error(Thread.currentThread().getName() + ": 用例内容为空");
        }
        undoPosition = undoDiffs.size();
        redoPosition = redoDiffs.size();
        lastUndoCounts = 0;
    }

    private TimerTask createBroadcastTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        broadcastTimerTick();
                    }
                });
            }
        };
    }

    public String getTestCaseContent() {
        return testCaseContent;
    }

    public void setTestCaseContent(String content) {
        testCaseContent = content;
    }

    public Player createAndAddPlayer(Client client) {
        if (players.size() >= MAX_PLAYER_COUNT) {
            throw new IllegalStateException("Maximum player count ("
                    + MAX_PLAYER_COUNT + ") has been reached.");
        }
        LOGGER.info(Thread.currentThread().getName() + ": 有新的用户加入。session id: " + client.getSession().getId());
        Player p = new Player(this, client);

        // 通知消息
//        broadcastRoomMessage(CaseMessageType.NOTIFY, "当前用户数:" + (players.size() + 1) + ",用户是:" + client.getClientName());

        players.add(p);
        cs.put(client.getSession(), client);

        // 如果广播任务没有被调度，则新建一个
        if (activeBroadcastTimerTask == null) {
            activeBroadcastTimerTask = createBroadcastTimerTask();
            messageBroadcastTimer.schedule(activeBroadcastTimerTask,
                    TIMER_DELAY, TIMER_DELAY);
        }

        // 发送当前用户数
        String content = String.valueOf(players.size());
        Set<String> names = new HashSet<>();
//        p.sendRoomMessageSync(CaseMessageType.NOTIFY, "当前用户数:" + content + ",用户是:" + client.getClientName());
//        broadcastRoomMessage(CaseMessageType.NOTIFY, "当前用户数:" + content + ",用户是:" + client.getClientName());
        for (Client c : cs.values()) {
            names.add(c.getClientName());
        }
        broadcastRoomMessage(CaseMessageType.NOTIFY, "当前用户数:" + content + ",用户是:" + String.join(",", names));

        return p;
    }

    protected void internalRemovePlayer(Player p) {

        boolean removed = players.remove(p);
        assert removed;

        cs.remove(p.getClient().getSession());
        LOGGER.info(Thread.currentThread().getName() + ": 有用户 " + p.getClient().getClientName() + " 离开 session id:" + p.getClient().getSession().getId());

        // 如果是最后一个用户离开，需要关闭广播任务
        if (players.size() == 0) {
            // 关闭任务
            // todo： 为了避免timer cancel时还有任务没有执行完，需要在查询invokeAndWait的任务信息
            closed = true;
            activeBroadcastTimerTask.cancel();
            activeBroadcastTimerTask = null;
        }

        // 广播有用户离开
//        broadcastRoomMessage("用户离开：" + p.getClient().getSession().getId());
    }

    // 直接广播发送内容，不经过buffer池。适用于所有消息都是一致的场景。
    protected void broadcastRoomMessage(CaseMessageType type, String content) {

        for (Player p : players) {
            p.sendRoomMessageSync(type, content);
        }
    }

    private void internalHandleMessage(Player p, String msg,
                                       long msgId) {
        p.setLastReceivedMessageId(msgId);

        if (editInfoSaveToDB) {
            CaseBackup caseBackup = new CaseBackup();
            caseBackup.setCaseContent(msg);
            caseBackup.setCaseId(p.getRoom().roomId);
            caseBackup.setTitle("edit from session: " + p.getClient().getSession().getId());
            caseBackup.setCreator(p.getClient().getClientName());
            caseBackup.setIsDelete(2); // 对编辑信息的内容进行特殊标记
            caseBackup.setRecordContent("");
            int ret = caseBackupService.insertEditInfo(caseBackup);
            if (ret < 1) {
                LOGGER.error("编辑过程备份落库失败. casebackup id: " + caseBackup.getCaseId() + ", case content: " +
                        caseBackup.getCaseContent());
            }
        } else {
            LOGGER.info(Thread.currentThread().getName() + ": 收到消息... onMessage: " + msg.trim());
        }

        //todo: testCase.apply(msg) 新增如上的方法.
        if (msg.endsWith("undo")) {
            undo();
            lastUndoCounts ++;
        } else if (msg.endsWith("redo")) {
            redo();
            lastUndoCounts --;
        } else {
            broadcastMessage(msg);

        }
    }

    private void undo() {
        roomLock.lock();
        if(undoPosition == 0)
            LOGGER.error("不能再进行undoPosition操作");
        else{
            try {
                undoPosition --;
                redoPosition --;
                broadcastRoomMessage(CaseMessageType.EDITOR, undoDiffs.get(undoPosition));
                JsonNode target = JsonPatch.apply(jsonMapper.readTree(undoDiffs.get(undoPosition)), jsonMapper.readTree(testCaseContent));
                testCaseContent = target.toString();
            } catch (Exception e) {
                roomLock.unlock();
                LOGGER.error("undo json parse error。", e);
                return;
            }
        }
        roomLock.unlock();
    }

    private void redo() {
        roomLock.lock();
        if(redoPosition == undoDiffs.size())
            LOGGER.error("不能再进行redoPosition操作");
        else{
            try {
                broadcastRoomMessage(CaseMessageType.EDITOR, redoDiffs.get(redoPosition));
                JsonNode target = JsonPatch.apply(jsonMapper.readTree(redoDiffs.get(undoPosition)), jsonMapper.readTree(testCaseContent));
                testCaseContent = target.toString();
            } catch (Exception e) {
                roomLock.unlock();
                LOGGER.error("redo json parse error。", e);
                return;
            }

            undoPosition ++;
            redoPosition ++;
        }

        roomLock.unlock();
    }

    private void internalHandleCtrlMessage(String msg) {
        int seperateIndex = msg.indexOf('|');
        String sendSessionId = msg.substring(0, seperateIndex);

        for (Player p : players) {
            if (sendSessionId.equals(p.getClient().getSession().getId())) {
                p.getBufferedMessages().add("2" + "success");
//                p.sendRoomMessage("2" + "success");
            } else {
                p.getBufferedMessages().add("2" + msg.substring(seperateIndex + 1));
//                p.sendRoomMessage("2" + "lock");
            }
        }
    }

    public void leavebroadcastMessageForHttp(String msg) {
        for (Player p : players) {
                p.getClient().sendMessage(CaseMessageType.EDITOR, msg);
        }
    }

    private void broadcastMessage(String msg) {
        if (!BUFFER_MESSAGES) {
            String msgStr = msg.toString();

            for (Player p : players) {
                String s = String.valueOf(p.getLastReceivedMessageId())
                        + "," + msgStr;
                p.sendRoomMessageSync(CaseMessageType.EDITOR, s); // 直接发送，不放到buffer
            }
        } else {
            int seperateIndex = msg.indexOf('|');
            String sendSessionId = msg.substring(0, seperateIndex);
            try {
                JsonNode request = jsonMapper.readTree(msg.substring(seperateIndex + 1));
                ArrayNode patch = (ArrayNode) request.get("patch");
                long currentVersion = ((JsonNode) request.get("case")).get("base").asLong();
                String tmpTestCaseContent = ((JsonNode) request.get("case")).toString().replace("\"base\":" + currentVersion, "\"base\":" + (currentVersion + 1));
                ArrayNode patchReverse = (ArrayNode) JsonDiff.asJson(jsonMapper.readTree(tmpTestCaseContent),
                        jsonMapper.readTree(testCaseContent==null?testCase.getCaseContent():testCaseContent), EnumSet.of(ADD_ORIGINAL_VALUE_ON_REPLACE, OMIT_MOVE_OPERATION));

                testCaseContent = tmpTestCaseContent;
                ArrayNode patchNew = patchTraverse(patch);

                ObjectNode basePatch = FACTORY.objectNode();
                basePatch.put("op", "replace");
                basePatch.put("path", "/base");
                basePatch.put("value", currentVersion + 1);
                patchNew.add(basePatch);
//                String msgNotify = patch.toString().replace("[[{", "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (currentVersion + 1) + "},{");
                redoDiffs.add(redoPosition++, patchNew.toString());
                undoDiffs.add(undoPosition++, patchReverse.toString());

                for (Player p : players) {
                    if (sendSessionId.equals(p.getClient().getSession().getId())) { //ack消息
                        String msgAck = "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (currentVersion + 1) + "}]]";
                        p.getBufferedMessages().add(msgAck);
                        p.undoCount ++;
                    } else { // notify消息
//                        String msgNotify = patch.toString().replace("[[{", "[[{\"op\":\"replace\",\"path\":\"/base\",\"value\":" + (currentVersion + 1) + "},{");
                        p.getBufferedMessages().add(patchNew.toString());
                        p.undoCount ++;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("json 操作失败。", e);
            }
        }
    }

    private ArrayNode patchTraverse(ArrayNode patch) {
        ArrayNode patchesNew = FACTORY.arrayNode();
        try {
            for (int i = 0; i < patch.size(); i++) {
                patchesNew.addAll((ArrayNode) patch.get(i));
            }
        } catch (Exception e) {
            LOGGER.error("转换客户端发送patch失败。", e);
        }
        return patchesNew;
    }

    private void broadcastTimerTick() {
        // 给每一个player广播消息
        for (Player p : players) {
            StringBuilder sb = new StringBuilder();
            List<String> caseMessages = p.getBufferedMessages();
//            LOGGER.info("当前的消息池消息数量为：" + caseMessages.size());
            if (caseMessages.size() > 0) {
                for (int i = 0; i < caseMessages.size(); i++) {
                    String msg = caseMessages.get(i);

//                    String s = String.valueOf(p.getLastReceivedMessageId())
//                            + "," + msg.toString();
                    if (i > 0) {
                        sb.append("|");
                        LOGGER.error(Thread.currentThread().getName() + ": client: " + p.getClient().getClientName() + " 此处可能会有问题，待处理 by肖锋. sb: " + sb);
                    }

                    sb.append(msg);
                }

                caseMessages.clear();

                p.sendRoomMessageSync(CaseMessageType.EDITOR, sb.toString());
            }
        }

    }

    private List<Runnable> cachedRunnables = null;

    public void invokeAndWait(Runnable task)  {

        // 检查当前线程是否持有房间锁，如果持有，则不能直接允许runnable，要先缓存住，直到前一个任务执行完成。
        if (roomLock.isHeldByCurrentThread()) {

            if (cachedRunnables == null) {
                cachedRunnables = new ArrayList<>();
            }
            cachedRunnables.add(task);

        } else {
            roomLock.lock();
            try {
                // 确保数据一致性。已经有任务执行时，会卡在下面的task.run，然后其他新进来的任务cache住
                cachedRunnables = null;

                if (!closed) {
                    task.run();
                }

                // 执行缓存的任务
                if (cachedRunnables != null) {
                    for (Runnable cachedRunnable : cachedRunnables) {
                        if (!closed) {
                            cachedRunnable.run();
                        }
                    }
                    cachedRunnables = null;
                }
            } finally {
                roomLock.unlock();
            }
        }
    }

    public String getRoomPlayersName() {
        Set<String> playerNames = new HashSet<>();
        for (Player p: players) {
            playerNames.add(p.getClient().getClientName());
        }
        return StringUtil.join(playerNames.toArray(), ",");
    }

    public static final class Player {

        /**
         * player所属的room
         */
        private Room room;

        /**
         * room缓存的最后一个msg id
         */
        private long lastReceivedMessageId = 0;

        private final Client client;
        private final long enterTimeStamp;

        private Integer pingCount;

        //        private Integer undoPosition;
//        private Integer redoPosition;
        private Integer undoCount;
        private Integer redoCount;

//        private final boolean isRecord;

        /**
         * 缓存的将要被timer处理的消息
         */
        private final List<String> bufferedMessages = new ArrayList<>();

        private List<String> getBufferedMessages() {
            return bufferedMessages;
        }

        public boolean isPingNormal() {
            return pingCount <= 2;
        }

        public void clearPingCount() {
            this.pingCount = 0;
        }

        private Player(Room room, Client client) {
            this.room = room;
            this.client = client;
            this.enterTimeStamp = System.currentTimeMillis();
            this.pingCount = 0;
//            this.undoPosition = room.undoDiffs.size();
//            this.redoPosition = room.redoDiffs.size();
            this.undoCount = 0;
            this.redoCount = 0;
//            isRecord = client.getRecordId();
        }

        public Boolean undo() {
            if (this.undoCount <= 0) {
                LOGGER.warn("当前用户未编辑过，无法进行undo。用户是：" + this.client.getClientName());
                return false;
            }
            this.undoCount --;
//            this.undoPosition --;
            this.redoCount ++;
//            this.redoPosition --;
            return true;
        }

        public Boolean redo() {
            if (this.redoCount <= 0) {
                LOGGER.warn("当前用户未undo过，无法进行redo。用户是：" + this.client.getClientName());
                return false;
            }
            this.undoCount ++;
//            this.undoPosition ++;
            this.redoCount --;
//            this.redoPosition ++;
            return true;
        }

        public Room getRoom() {
            return room;
        }

        public Client getClient() {
            return client;
        }

        /**
         * client断开连接时，需要移除的player
         */
        public void removeFromRoom() {
            if (room != null) {
                LOGGER.info("当前离开用户 " + this.getClient().getClientName() +  " 的使用时长是：" + String.valueOf(System.currentTimeMillis() - this.enterTimeStamp));
                room.internalRemovePlayer(this);
                room = null;
            }
        }

        private long getLastReceivedMessageId() {
            return lastReceivedMessageId;
        }
        private void setLastReceivedMessageId(long value) {
            lastReceivedMessageId = value;
        }


        /**
         * 处理客户端发送的消息，并将消息广播给所有players
         *
         * @param msg   接收的消息
         * @param msgId 消息id
         */
        public void handleMessage(String msg, long msgId) {
            room.internalHandleMessage(this, msg, msgId);
        }

        public void handleCtrlMessage(String msg) {
            room.internalHandleCtrlMessage(msg);
        }

        /**
         * 发送room的消息
         * @param content
         */
        public void sendRoomMessageSync(CaseMessageType type, String content) {
            Objects.requireNonNull(content);
            if (content.equals(CaseWsMessages.PING.getMsg())) {
                this.pingCount ++;
                if (!isPingNormal()) {
                    LOGGER.error("服务端ping客户端3次失败，当前用户连接有问题：" + this.getClient().getClientName());
                    throw new RuntimeException("心跳错误，客户端与服务端连接错误");
                }
            }
            client.sendMessage(type, content);
        }

        public void sendRoomMessageAsync(String content) {
            Objects.requireNonNull(content);

            this.getBufferedMessages().add(content);
        }
    }
}

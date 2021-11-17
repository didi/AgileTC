package com.xiaoju.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by didi on 2021/3/22.
 */
public class Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private final Session session;
    private final RemoteEndpoint.Async async;


    private final LinkedList<String> messagesToSend = new LinkedList<>();

    private volatile boolean isSendingMessage = false;

    private volatile boolean isClosing = false;
//    private final boolean isRecord;
    private final Long recordId;
    private final String clientName;

    private volatile long messagesToSendLength = 0;

    public Client(Session session, String recordId, String user) {
        this.session = session;
        this.async = session.getAsyncRemote();
        this.recordId = recordId.equals(CaseWsMessages.UNDEFINED.getMsg()) ? 0L : Long.valueOf(recordId);
        this.clientName = user;
    }

    public String getClientName() {
        return this.clientName;
    }
    public Session getSession() {
        return session;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void close() {
        LOGGER.info(Thread.currentThread().getName() + ": client " + this.getClientName() + " 准备退出。" + this.session.getId());
        sendMessage(CaseMessageType.CTRL, new String(CaseWsMessages.CLIENT_CLOSE.getMsg()));
    }

    public void sendMessage(CaseMessageType type, String msg) {
        if (!type.equals(CaseMessageType.PING)) {
            LOGGER.info(Thread.currentThread().getName() + ": 准备向 " + this.getClientName() + " 发送消息");
        }
        synchronized (messagesToSend) {
            if (!isClosing) {
                // 检查当前是否是关闭消息
                if (msg.contains(CaseWsMessages.CLIENT_CLOSE.getMsg())) {
                    isClosing = true;
                }

                if (isSendingMessage) {
                    LOGGER.warn(Thread.currentThread().getName() + ": 消息正在发送中，先缓存住. ");
                    // 检查缓存消息大小或数量是否异常
                    if (messagesToSend.size() >= 1000
                            || messagesToSendLength >= 1000000) {
                        isClosing = true;

                        // 丢掉异常的消息，关闭session
                        CloseReason cr = new CloseReason(
                                CloseReason.CloseCodes.VIOLATED_POLICY,
                                "Send Buffer exceeded");
                        try {
                            // TODO: 如果客户端没有读取到数据，close()可能会阻塞，最终会抛一个超时异常。
                            // 不管用什么方式，这个方法（sendMessage）需要异步执行，不能阻塞。否则可能
                            // 短时间无法处理消息。或者另起一个线程执行这个方法。
                            // 注意：当这个方法执行时，RemoteEndpoint.Async依旧在发送数据，因此需要有
                            // 一个方法关闭websocket链接。理想情况是，需要有方法可以直接关闭链接。
                            session.close(cr);
                        } catch (IOException e) {
                            LOGGER.error(Thread.currentThread().getName() + ": 接收的信息异常，关闭session.", e);
                        }

                    } else {
                        // 验证最后的消息和新的消息是否都是String。如果是，可以拼接起来，降低网络损耗。
                        if (!messagesToSend.isEmpty()) {

                            String ms = messagesToSend.removeLast();
                            messagesToSendLength -= (ms.length());

                            String concatenated = ms + ";" + msg;
                            msg = new String(concatenated);
                        }

                        messagesToSend.add(msg);
                        messagesToSendLength += (msg.length());
                    }
                } else {
                    isSendingMessage = true;
                    internalSendMessageAsync(msg);
                }
            }

        }
    }

    private void internalSendMessageAsync(String msg) {
        try {
            if (msg instanceof String) {
                async.sendText(msg, sendHandler);
            } else if (msg.contains(CaseWsMessages.CLIENT_CLOSE.getMsg())) {
                LOGGER.info(Thread.currentThread().getName() + ": 消息中包含关闭信息，关闭session. ");
                session.close();
            }
        } catch (IllegalStateException|IOException e) {
            LOGGER.error(Thread.currentThread().getName() + ": Client发送消息到session过程中，session被关闭。", e);
        }
    }

    private final SendHandler sendHandler = new SendHandler() {
        @Override
        public void onResult(SendResult result) {
            if (!result.isOK()) {
                // 消息无法发送. 在这种情况下，不将isSendingMessage置false，因为这时必须假定链接断开
                // （onClose会被调用），因此不需要尝试发送其他信息。
                // 不过，以防万一，还是关闭session（例如，发送超时）
                // TODO: session.close() 阻塞, 这个handler不应该被阻塞
                // 理想情况是，需要有方法可以直接关闭链接。
                try {
                    session.close();
                } catch (IOException ex) {
                    // 忽略
                    LOGGER.error(Thread.currentThread().getName() + ": client " + session.getId() + " 中session关闭异常.");
                }
            }
            synchronized (messagesToSend) {
                if (!messagesToSend.isEmpty()) {
                    String msg = messagesToSend.remove();
                    messagesToSendLength -= (msg.length());

                    internalSendMessageAsync(msg);

                } else {
                    isSendingMessage = false;
                }
            }
        }
    };
}

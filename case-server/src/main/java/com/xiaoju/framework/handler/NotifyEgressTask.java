package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;

public class NotifyEgressTask extends EgressTask{

    BroadcastOperations broadcastOperations;

    public NotifyEgressTask(String name, PushMessage egressMsg, BroadcastOperations broadcastOperations) {
        super(name, egressMsg);
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public void run() {
        LOGGER.info("notify egress message: " + egressMsg);
        broadcastOperations.sendEvent(name, egressMsg);
    }
}

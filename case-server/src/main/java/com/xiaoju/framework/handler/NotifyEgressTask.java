package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;

public class NotifyEgressTask extends EgressTask{
//    String name;
//    String egressMsg;
    BroadcastOperations broadcastOperations;

    public NotifyEgressTask(String name, PushMessage egressMsg, BroadcastOperations broadcastOperations) {
        super(name, egressMsg);
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public void run() {
        broadcastOperations.sendEvent(name, egressMsg);
    }
}

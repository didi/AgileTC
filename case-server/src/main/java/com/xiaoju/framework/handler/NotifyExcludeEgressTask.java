package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;

public class NotifyExcludeEgressTask extends EgressTask {
    BroadcastOperations broadcastOperations;
    SocketIOClient client;

    public NotifyExcludeEgressTask(String name, PushMessage egressMsg, SocketIOClient client, BroadcastOperations broadcastOperations) {
        super(name, egressMsg);
        this.client = client;
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public void run() {
        broadcastOperations.sendEvent(name, client, egressMsg);
    }
}

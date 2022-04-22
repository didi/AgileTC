package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;

public abstract class EgressTask implements Runnable{
    String name;
    PushMessage egressMsg;
//    BroadcastOperations broadcastOperations;
//    SocketIOClient excludeClient;

    public EgressTask(String name, PushMessage egressMsg) {
        this.name = name;
        this.egressMsg = egressMsg;
//        this.broadcastOperations = broadcastOperations;
//        this.excludeClient = client;
    }

    @Override
    public void run() {
//        if (excludeClient == null) {
//            broadcastOperations.sendEvent(name, egressMsg);
//        } else {
//            broadcastOperations.sendEvent(name, excludeClient, egressMsg);
//        }
    }
}

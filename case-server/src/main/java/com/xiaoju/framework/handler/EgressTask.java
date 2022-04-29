package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EgressTask implements Runnable{
    protected static final Logger LOGGER = LoggerFactory.getLogger(EgressTask.class);

    String name;
    PushMessage egressMsg;

    public EgressTask(String name, PushMessage egressMsg) {
        this.name = name;
        this.egressMsg = egressMsg;
    }

    @Override
    public void run() {
        LOGGER.info("egress message: " + egressMsg);
//        if (excludeClient == null) {
//            broadcastOperations.sendEvent(name, egressMsg);
//        } else {
//            broadcastOperations.sendEvent(name, excludeClient, egressMsg);
//        }
    }
}

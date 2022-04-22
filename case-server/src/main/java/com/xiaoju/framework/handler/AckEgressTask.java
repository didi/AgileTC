package com.xiaoju.framework.handler;

import com.corundumstudio.socketio.SocketIOClient;

public class AckEgressTask extends EgressTask{
    SocketIOClient client;

    public AckEgressTask(String name, PushMessage egressMsg, SocketIOClient client) {
        super(name, egressMsg);
        this.client = client;
    }

    @Override
    public void run() {
        client.sendEvent(name, egressMsg);
    }
}

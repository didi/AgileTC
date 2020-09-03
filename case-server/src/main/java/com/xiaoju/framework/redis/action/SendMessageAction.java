package com.xiaoju.framework.redis.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.handler.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class SendMessageAction implements Action{
    @Autowired
    WebSocket webSocket;

    @Override
    public void doMessage(JSONObject object) {
        String caseId = object.getString(Action.CASEID);
        String recordId = object.getString(Action.RECORDID);
        String sessionId = object.getString(Action.SESSIONID);
        JSONObject request = JSON.parseObject(object.getString(Action.MESSAGE));

        JSONArray patch = (JSONArray)request.get("patch");
        long currentVersion = ((JSONObject)request.get("case")).getLong("base");

        //本地session同步消息
        webSocket.sendMessageToOthers(patch,currentVersion,caseId,recordId,sessionId);
    }
}

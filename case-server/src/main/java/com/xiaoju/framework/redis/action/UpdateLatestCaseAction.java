package com.xiaoju.framework.redis.action;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.handler.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateLatestCaseAction implements Action {
    @Autowired
    WebSocket webSocket;

    @Override
    public void doMessage(JSONObject object) {
        String caseId = object.getString(Action.CASEID);
        String recordId = object.getString(Action.RECORDID);
        String isCore = object.getString(Action.isCore);
        String user = object.getString(Action.USER);

        //本地保存最新内容
        webSocket.updateLatestCase(caseId,recordId,isCore,user);
    }
}

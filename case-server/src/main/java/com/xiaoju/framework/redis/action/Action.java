package com.xiaoju.framework.redis.action;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.redis.RedisWebSocketManager;

public interface Action {
    String CASEID = "caseID";
    String RECORDID = "recordID";
    String SESSIONID = "sessionID";
    String isCore = "isCore";
    String USER = "user";
    String MESSAGE = "message";
    String ACTION = "action";

    void doMessage(JSONObject object);
}

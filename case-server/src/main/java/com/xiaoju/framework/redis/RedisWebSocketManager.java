package com.xiaoju.framework.redis;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.redis.action.Action;
import com.xiaoju.framework.redis.action.SendMessageAction;
import com.xiaoju.framework.redis.action.UpdateLatestCaseAction;
import com.xiaoju.framework.redis.action.UserAction;
import com.xiaoju.framework.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理redis频道消息发送
 */
@Slf4j
@Service
public class RedisWebSocketManager {
    public static final String Websocket_channel = "CaseServerWebsocket";//订阅的频道名称为 websocket--消息传递

    public StringRedisTemplate stringRedisTemplate = SpringUtils.getBean(StringRedisTemplate.class); //redis的string消息媒介

    /**
     * 同步消息
     * @param message
     */
    public void sendRedisMessage(String caseId,String recordId,String sessionId,String message){
        Map<String , Object> map = new HashMap<>(2);
        map.put(Action.CASEID,caseId);
        map.put(Action.RECORDID,recordId);
        map.put(Action.SESSIONID,sessionId);
        map.put(Action.ACTION, SendMessageAction.class.getName());
        map.put(Action.MESSAGE,message);

        stringRedisTemplate.convertAndSend(Websocket_channel, new JSONObject(map).toString());
    }

    /**
     * 更新目标用例/记录为最新内容
     */
    public void updateCaseMessage(String caseId,String recordId,String isCore,String user){
        Map<String , Object> map = new HashMap<>(2);
        map.put(Action.CASEID,caseId);
        map.put(Action.RECORDID,recordId);
        map.put(Action.isCore,isCore);
        map.put(Action.USER,user);
        map.put(Action.ACTION, UpdateLatestCaseAction.class.getName());

        stringRedisTemplate.convertAndSend(Websocket_channel, new JSONObject(map).toString());
    }

    /**
     * 访问用户记录——增加一个
     */
    public void addUser(String user){
        Map<String , Object> map = new HashMap<>(2);
        map.put(Action.USER,user);
        map.put(Action.ACTION, UserAction.class.getName());
        map.put(Action.MESSAGE,"add");

        stringRedisTemplate.convertAndSend(Websocket_channel, new JSONObject(map).toString());
    }

    /**
     * 访问用户记录——减少一个
     * @param user
     */
    public void delUser(String user){
        Map<String , Object> map = new HashMap<>(2);
        map.put(Action.USER,user);
        map.put(Action.ACTION, UserAction.class.getName());
        map.put(Action.MESSAGE,"del");

        stringRedisTemplate.convertAndSend(Websocket_channel, new JSONObject(map).toString());
    }
}

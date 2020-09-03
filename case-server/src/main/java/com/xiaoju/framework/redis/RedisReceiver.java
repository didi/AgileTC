package com.xiaoju.framework.redis;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.redis.action.Action;
import com.xiaoju.framework.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 消息订阅者
 */
@Component
@Slf4j
public class RedisReceiver {

    //接收频道消息根据动作处理
    public void receiveMessage(String message, String channel) {
        log.info(channel + " 收到消息: " + message);

        if(channel.equals(RedisWebSocketManager.Websocket_channel)){
            JSONObject object = JSONObject.parseObject(message);
            if(!object.containsKey(Action.ACTION)){
                return;
            }
            String actionName = object.getString(Action.ACTION);
            Action action = getAction(actionName);
            action.doMessage(object);
        }else {
            log.info("非ws频道");
        }
    }

    private Action getAction(String actionName) {
        return (Action) SpringUtils.getBean(actionName);
    }

}

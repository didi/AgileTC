package com.xiaoju.framework.redis.action;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.handler.WebSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserAction implements Action{

    @Override
    public void doMessage(JSONObject object) {

        String user = object.getString(Action.USER);
        String action = object.getString(Action.MESSAGE);

        if(action.equals("add")){
            log.info("user:"+user+",新链接ws");

            if(WebSocket.userInfo.containsKey(user)){
                int num = WebSocket.userInfo.get(user)+1;
                log.info("contains:"+num);
                WebSocket.userInfo.replace(user,num);
            }else{
                WebSocket.userInfo.put(user,1);
            }

        }else if(action.equals("del")){
            log.info("user:"+user+",关闭一ws");

            if(WebSocket.userInfo.containsKey(user)){
                int num = WebSocket.userInfo.get(user);
                if(num==1){
                    WebSocket.userInfo.remove(user);
                }else {
                    WebSocket.userInfo.replace(user,num-1);
                }
            }else {
                log.info("在线用户数减少error");
            }

        }

        log.info("当前在线用户信息为："+WebSocket.userInfo.toString());
    }
}

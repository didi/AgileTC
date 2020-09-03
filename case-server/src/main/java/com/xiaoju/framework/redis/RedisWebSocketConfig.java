//package com.xiaoju.framework.redis;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.listener.PatternTopic;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
//
//@Configuration
//public class RedisWebSocketConfig {
//    @Bean
//    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        return new StringRedisTemplate(redisConnectionFactory);
//    }
//
//    @Bean
//    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
//                                            MessageListenerAdapter listenerAdapter) {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();//消息监听容器
//        container.setConnectionFactory(connectionFactory);
//        container.addMessageListener(listenerAdapter, new PatternTopic(RedisWebSocketManager.Websocket_channel));//监听渠道
//        return container;
//    }
//
//    @Bean
//    MessageListenerAdapter listenerAdapter(RedisReceiver redisReceiver) {
//        return new MessageListenerAdapter(redisReceiver, "receiveMessage");
//    }
//
//}

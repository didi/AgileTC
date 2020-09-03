package com.xiaoju.framework.redis.action;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SendMessageAction.class, UpdateLatestCaseAction.class,UserAction.class})
public class ActionConfig {
}

package com.xiaoju.framework.auth.entity;

import lombok.Data;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * @author littleforestjia
 * @description
 * @date 2021/3/29 14:55:21
 */
@Data
public class AgileToken implements AuthenticationToken {

    private static final long serialVersionUID = 5288207035894094853L;

    private String username;

    private Integer channel;

    private Long lineId;

    public AgileToken(String username, String channel, String lineId) {
        this.username = username;
        this.channel = Integer.parseInt(channel);
        this.lineId = Long.parseLong(lineId);
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public Object getCredentials() {
        return this;
    }
}

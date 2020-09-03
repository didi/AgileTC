package com.xiaoju.framework.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SendDchatParam {
    private String dchatUrl;
    private List<String> receivers;
    private String content;
}
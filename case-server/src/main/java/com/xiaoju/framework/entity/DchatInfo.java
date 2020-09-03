package com.xiaoju.framework.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class DchatInfo {
    private String sendDchatApi;
    private String token;
    private List<String> toUsers;
    private String title;
    private String content;
}

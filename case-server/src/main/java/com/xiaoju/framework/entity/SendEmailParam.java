package com.xiaoju.framework.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SendEmailParam {
    private String emailUrl;
    private String title;
    private List<String> toUsers;
    private String content;
}

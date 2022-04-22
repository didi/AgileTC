package com.xiaoju.framework.handler;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushMessage {
//    @ApiModelProperty(value = "登录用户编号")
    private String userName;

//    @ApiModelProperty(value = "推送内容")
    private String message;
    // Other Detail Property...
}

package com.xiaoju.framework.handler;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EditMessage {

    //    @ApiModelProperty(value = "登录用户编号")
    private String caseContent;

    //    @ApiModelProperty(value = "推送内容")
    private String patch;
    // Other Detail Property...

    private Integer caseVersion;
}

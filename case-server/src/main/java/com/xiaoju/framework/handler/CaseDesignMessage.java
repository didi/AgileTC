package com.xiaoju.framework.handler;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CaseDesignMessage {
    String method;

    String nodeId;

    String message;
}

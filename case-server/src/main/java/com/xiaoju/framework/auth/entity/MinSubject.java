package com.xiaoju.framework.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一传参体，最小场景下的某个人
 *
 * @author didi
 * @date 2021/2/2
 */
@Data
@AllArgsConstructor
public class MinSubject {
    private String username;
    private Long lineId;
    private Integer channel;
}

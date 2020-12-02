package com.xiaoju.framework.entity.xmind;

import lombok.Data;

import java.util.List;

/**
 * 用例的json单元
 *
 * @author hcy
 * @date 2020/8/13
 */
@Data
public class RootData {

    private DataObj data;

    private List<RootData> children;

}

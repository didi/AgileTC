package com.xiaoju.framework.entity.response.dir;

import lombok.Data;

import java.util.List;

/**
 * 目录结构
 *
 * @author hcy
 * @date 2020/9/4
 */
@Data
public class BizNodeResp {

    /**
     * 文件夹id
     */
    private Long bizId;

    /**
     * 文件夹名称
     */
    private String bizName;

    /**
     * 树编号
     * 如果传出-1 表示是<未分类的用例>
     */
    private Integer treeNo;

    /**
     * 业务线id
     */
    private Long lineId;

    /**
     * 子列表
     */
    private List<BizNodeResp> childs;

}

package com.xiaoju.framework.entity.request.record;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务 查询列表
 *
 * @author hcy
 * @date 2020/8/24
 */
@Data
public class RecordQueryReq {

    private Integer channel;

    private List<Long> reqIds;

    private Integer pageNum;

    private Integer pageSize;

    public RecordQueryReq(Integer channel, String[] reqIds, Integer pageNum, Integer pageSize) {
        // String[]转为List<Long>
        List<Long> reqIdList = new ArrayList<>();
        for (String reqId : reqIds) {
            reqIdList.add(Long.valueOf(reqId));
        }
        this.channel = channel;
        this.reqIds = reqIdList;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}

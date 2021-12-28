package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.persistent.Biz;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 文件夹映射
 *
 * @author didi
 * @date 2020/9/9
 * @see Biz
 */
@Repository
public interface BizMapper {

    /**
     * 插入文件夹
     *
     * @param biz 文件夹实体
     * @return bizId
     */
    int insert(Biz biz);

    /**
     * 通过主键修改文件夹
     *
     * @param biz 文件夹实体
     * @return bizId
     */
    int update(Biz biz);

    /**
     * 根据渠道和业务线获取文件夹
     *
     * @param productLineId 业务线id
     * @param channel 渠道
     * @return 文件夹实体
     */
    Biz selectOne(@Param("productLineId")Long productLineId, @Param("channel")Integer channel);

    /**
     * 更新文件夹树内容
     *
     * @param productLineId 业务线id
     * @param content 文件夹内容
     * @param channel 渠道
     */
    void updateContent(@Param("productLineId")Long productLineId, @Param("content")String content, @Param("channel")Integer channel);

}

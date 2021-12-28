package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.dto.Authority;

public interface AuthorityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Authority record);

    int insertSelective(Authority record);

    Authority selectByPrimaryKey(Long id);

    /**
     * 通过权限名称查询最新一条
     * @param authorityName 权限名称
     * @return 权限信息
     */
    Authority selectByAuthorityName(String authorityName);

    int updateByPrimaryKeySelective(Authority record);

    int updateByPrimaryKey(Authority record);
}
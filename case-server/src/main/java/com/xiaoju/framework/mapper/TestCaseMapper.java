package com.xiaoju.framework.mapper;

import com.xiaoju.framework.entity.TestCase;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TestCaseMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TestCase record);

    TestCase selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TestCase record);

    int updateByPrimaryKeyWithBLOBs(TestCase record);

    int updateByPrimaryKey(TestCase record);

    List<TestCase> listByRequirementId(Long requirementId);
    TestCase selectNameById(Long id);
    int getCountTestCase(@Param("productLineId") Long productLineId,
                         @Param("case_type") Integer case_type,
                         @Param("id") Long id,
                         @Param("title") String title,
                         @Param("creator") String creator,
                         @Param("requirement_id") String[] requirement_id,
                         @Param("beginTime") Date beginTime,
                         @Param("endTime")  Date endTime,
                         @Param("channel")   Integer channel);
    List<TestCase> listTestCaseByids(@Param("productLineId") Long productLineId,
                                     @Param("case_type") Integer case_type,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize,
                                     @Param("id") Long id,
                                     @Param("title") String title,
                                     @Param("creator") String creator,
                                     @Param("requirement_id") String[] requirement_id,
                                     @Param("beginTime") Date beginTime,
                                     @Param("endTime")  Date endTime,
                                     @Param("channel")   Integer channel);
    List<String> listCreators(@Param("case_type") Integer case_type,@Param("productLineId")Long productLineId);

    List<Long> listProductLineId();

    int updateRequirementId(@Param("id") Long id,@Param("requirement_id") String requirement_id);
}
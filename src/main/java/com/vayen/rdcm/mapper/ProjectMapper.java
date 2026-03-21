package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目 Mapper 接口
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    
    /**
     * 按状态查询项目
     */
    List<Project> findByStatus(@Param("status") String status);
    
    /**
     * 根据创建人查询项目
     */
    List<Project> findByCreatedBy(@Param("createdBy") Long createdBy);
    
    /**
     * 结束项目
     */
    int finishProject(@Param("id") Long id, @Param("endTime") LocalDate endTime);
    
    /**
     * 删除项目
     */
    int delete(@Param("id") Long id);
}

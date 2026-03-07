package com.vayen.rdcm.mapper;

import com.vayen.rdcm.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目 Mapper 接口
 */
@Mapper
public interface ProjectMapper {
    
    /**
     * 查询所有项目
     */
    List<Project> findAll();
    
    /**
     * 按状态查询项目
     */
    List<Project> findByStatus(@Param("status") String status);
    
    /**
     * 根据创建人查询项目
     */
    List<Project> findByCreatedBy(@Param("createdBy") Long createdBy);
    
    /**
     * 根据 ID 查询项目
     */
    Project findById(@Param("id") Long id);
    
    /**
     * 插入项目
     */
    int insert(Project project);
    
    /**
     * 更新项目
     */
    int update(Project project);
    
    /**
     * 结束项目
     */
    int finishProject(@Param("id") Long id, @Param("endTime") LocalDate endTime);
    
    /**
     * 删除项目
     */
    int delete(@Param("id") Long id);
}

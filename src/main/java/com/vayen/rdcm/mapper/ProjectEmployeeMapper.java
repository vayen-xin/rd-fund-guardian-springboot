package com.vayen.rdcm.mapper;

import com.vayen.rdcm.entity.ProjectEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目 - 员工关联 Mapper 接口
 */
@Mapper
public interface ProjectEmployeeMapper {
    
    /**
     * 根据项目 ID 查询关联员工
     */
    List<ProjectEmployee> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据员工 ID 查询关联项目
     */
    List<ProjectEmployee> findByEmployeeId(@Param("employeeId") Long employeeId);
    
    /**
     * 批量插入项目 - 员工关联
     */
    int batchInsert(@Param("list") List<ProjectEmployee> list);
    
    /**
     * 删除项目关联的员工
     */
    int deleteByProjectId(@Param("projectId") Long projectId);
}

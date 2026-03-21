package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.ProjectEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目员工 Mapper（扩展方法）
 */
@Mapper
public interface ProjectEmployeeMapper extends BaseMapper<ProjectEmployee> {
    
    /**
     * 查询项目下所有员工
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM project_employee WHERE project_id = #{projectId} ORDER BY employee_name")
    List<ProjectEmployee> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 删除项目下所有员工（用于项目结束或删除时清理）
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM project_employee WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Long projectId);
}

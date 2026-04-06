package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目月度费用数据 Mapper
 */
@Mapper
public interface ProjectMonthlyDataMapper extends BaseMapper<ProjectMonthlyData> {
    
    /**
     * 根据项目ID和月份查询
     */
    ProjectMonthlyData findByProjectIdAndMonth(@Param("projectId") Long projectId, 
                                               @Param("workMonth") LocalDate workMonth);
    
    /**
     * 查询项目所有月度数据
     */
    List<ProjectMonthlyData> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 查询公司在某月的所有项目月度数据
     */
    List<ProjectMonthlyData> findByCompanyIdAndMonth(@Param("companyId") Long companyId,
                                                     @Param("workMonth") LocalDate workMonth);
}

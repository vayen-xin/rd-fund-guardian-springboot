package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目月度费用数据服务
 */
@Service
public interface ProjectMonthlyDataService extends IService<ProjectMonthlyData> {
    
    /**
     * 获取项目某月数据
     */
    ProjectMonthlyData getProjectMonthlyData(Long projectId, LocalDate workMonth);
    
    /**
     * 创建或更新月度数据
     */
    void saveMonthlyData(Long projectId, LocalDate workMonth, String costData,
                         String employeeData, Double grandTotal, Long createdBy);

    void submitMonthlyData(Long projectId, LocalDate workMonth, Long operatorId);
    
    /**
     * 触发月度数据继承（从指定月份复制到新月份）
     */
    void inheritFromLastMonth(Long projectId, LocalDate fromMonth, LocalDate toMonth, 
                              Long operatorId);
    
    /**
     * 获取项目所有月度数据（按月份倒序）
     */
    List<ProjectMonthlyData> getProjectMonthlyList(Long projectId);
}

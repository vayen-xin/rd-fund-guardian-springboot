package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vayen.rdcm.entity.ProjectMonthlyData;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目月度费用服务。
 */
public interface ProjectMonthlyDataService extends IService<ProjectMonthlyData> {

    ProjectMonthlyData getProjectMonthlyData(Long projectId, LocalDate workMonth);

    void saveMonthlyData(Long projectId, LocalDate workMonth, String costData,
                         String employeeData, String deviceData, Double grandTotal, Long createdBy);

    void submitMonthlyData(Long projectId, LocalDate workMonth, Long operatorId);

    void inheritFromLastMonth(Long projectId, LocalDate fromMonth, LocalDate toMonth, Long operatorId);

    List<ProjectMonthlyData> getProjectMonthlyList(Long projectId);
}

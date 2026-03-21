package com.vayen.rdcm.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.service.ProjectMonthlyDataService;
import com.vayen.rdcm.util.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 项目月度费用数据服务实现
 */
@Service
public class ProjectMonthlyDataServiceImpl extends ServiceImpl<ProjectMonthlyDataMapper, ProjectMonthlyData> 
    implements ProjectMonthlyDataService {
    
    /**
     * 将 LocalDate (yyyy-MM-01) 转换为 LocalDateTime (yyyy-MM-01 00:00:00)
     */
    private LocalDateTime toStartOfMonth(LocalDate month) {
        return month.atTime(0, 0, 0);
    }
    
    @Override
    public ProjectMonthlyData getProjectMonthlyData(Long projectId, LocalDate workMonth) {
        LocalDateTime monthStart = toStartOfMonth(workMonth);
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
               .eq("work_month", monthStart);
        return this.getOne(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMonthlyData(Long projectId, LocalDate workMonth, String costData, 
                                Double grandTotal, Long createdBy) {
        LocalDateTime monthStart = toStartOfMonth(workMonth);
        
        ProjectMonthlyData monthlyData = getProjectMonthlyData(projectId, workMonth);
        if (monthlyData == null) {
            monthlyData = new ProjectMonthlyData();
            monthlyData.setProjectId(projectId);
            monthlyData.setWorkMonth(monthStart);
            monthlyData.setCreatedBy(createdBy);
        }
        
        // 验证：JSON中的总和与传入的grandTotal一致
        double calculatedTotal = JsonUtils.calculateGrandTotal(costData);
        double diff = Math.abs(calculatedTotal - grandTotal);
        if (diff > 0.01) {
            throw new RuntimeException(String.format(
                "JSON计算总和(%.2f)与传入总金额(%.2f)不一致", calculatedTotal, grandTotal));
        }
        
        monthlyData.setCostData(costData);
        monthlyData.setGrandTotal(grandTotal);
        monthlyData.setUpdatedAt(LocalDateTime.now());
        
        // 计算并保存各冗余字段
        JsonUtils.CostData parsed = JsonUtils.parseCostData(costData);
        monthlyData.setLaborTotal(parsed.getCategoryTotal("labor"));
        monthlyData.setDirectMaterialTotal(parsed.getCategoryTotal("direct_material"));
        monthlyData.setDirectFuelTotal(parsed.getCategoryTotal("direct_fuel"));
        monthlyData.setDirectRentalTotal(parsed.getCategoryTotal("direct_rental"));
        monthlyData.setDepreciationTotal(parsed.getCategoryTotal("depreciation"));
        monthlyData.setAmortizationTotal(parsed.getCategoryTotal("amortization"));
        monthlyData.setDesignTotal(parsed.getCategoryTotal("design"));
        monthlyData.setCommissioningTotal(parsed.getCategoryTotal("commissioning"));
        monthlyData.setOutsourcedTotal(parsed.getCategoryTotal("outsourced"));
        monthlyData.setOtherTotal(parsed.getCategoryTotal("other"));
        monthlyData.setStatus("finalized");
        
        this.saveOrUpdate(monthlyData);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inheritFromLastMonth(Long projectId, LocalDate fromMonth, LocalDate toMonth, 
                                      Long operatorId) {
        LocalDateTime fromMonthStart = toStartOfMonth(fromMonth);
        LocalDateTime toMonthStart = toStartOfMonth(toMonth);
        
        // 1. 查询上个月数据
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
               .eq("work_month", fromMonthStart);
        ProjectMonthlyData lastMonthData = this.getOne(wrapper);
        
        if (lastMonthData == null) {
            // 上个月无数据，创建空记录
            ProjectMonthlyData newData = new ProjectMonthlyData();
            newData.setProjectId(projectId);
            newData.setWorkMonth(toMonthStart);
            newData.setCostData("{}");
            newData.setGrandTotal(0.0);
            newData.setLaborTotal(0.0);
            newData.setDirectMaterialTotal(0.0);
            newData.setDirectFuelTotal(0.0);
            newData.setDirectRentalTotal(0.0);
            newData.setDepreciationTotal(0.0);
            newData.setAmortizationTotal(0.0);
            newData.setDesignTotal(0.0);
            newData.setCommissioningTotal(0.0);
            newData.setOutsourcedTotal(0.0);
            newData.setOtherTotal(0.0);
            newData.setStatus("draft");
            newData.setCreatedBy(operatorId);
            newData.setCreatedAt(LocalDateTime.now());
            newData.setUpdatedAt(LocalDateTime.now());
            this.save(newData);
            return;
        }
        
        // 2. 复制JSON，金额清零
        String oldCostData = lastMonthData.getCostData();
        String newCostData = JsonUtils.zeroOutAmounts(oldCostData);
        
        ProjectMonthlyData newData = new ProjectMonthlyData();
        newData.setProjectId(projectId);
        newData.setWorkMonth(toMonthStart);
        newData.setCostData(newCostData);
        newData.setGrandTotal(0.0);
        newData.setLaborTotal(0.0);
        newData.setDirectMaterialTotal(0.0);
        newData.setDirectFuelTotal(0.0);
        newData.setDirectRentalTotal(0.0);
        newData.setDepreciationTotal(0.0);
        newData.setAmortizationTotal(0.0);
        newData.setDesignTotal(0.0);
        newData.setCommissioningTotal(0.0);
        newData.setOutsourcedTotal(0.0);
        newData.setOtherTotal(0.0);
        newData.setStatus("draft");
        newData.setCreatedBy(operatorId);
        newData.setCreatedAt(LocalDateTime.now());
        newData.setUpdatedAt(LocalDateTime.now());
        
        this.save(newData);
    }
    
    @Override
    public List<ProjectMonthlyData> getProjectMonthlyList(Long projectId) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
               .orderByDesc("work_month");
        return this.list(wrapper);
    }
}

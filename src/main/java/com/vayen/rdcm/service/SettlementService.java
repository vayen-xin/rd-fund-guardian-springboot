package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.entity.ProjectSettlement;
import com.vayen.rdcm.mapper.ProjectSettlementMapper;
import com.vayen.rdcm.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 结算服务，负责月度数据与结算记录的状态流转。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal OTHER_COST_RATIO_LIMIT = BigDecimal.valueOf(0.20);
    private static final BigDecimal OUTSOURCED_DISCOUNT = BigDecimal.valueOf(0.80);

    private final ProjectMonthlyDataService monthlyDataService;
    private final ProjectSettlementMapper settlementMapper;

    private LocalDateTime toStartOfMonth(LocalDate month) {
        return month.atTime(0, 0, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectSettlement createSettlement(Long projectId, LocalDate settlementMonth, Long operatorId) {
        log.info("用户 {} 发起项目结算，projectId={}，month={}", operatorId, projectId, settlementMonth);
        LocalDateTime settlementMonthStart = toStartOfMonth(settlementMonth);

        ProjectMonthlyData monthlyData = monthlyDataService.getProjectMonthlyData(projectId, settlementMonth);
        if (monthlyData == null) {
            throw new RuntimeException("当月无数据，请先录入费用");
        }
        if (!"finalized".equals(monthlyData.getStatus())) {
            throw new RuntimeException("月度数据未处于待结算状态，不能结算");
        }

        validateOtherCostRatio(monthlyData);
        validateOutsourcedCost(monthlyData);

        ProjectSettlement settlement = findSettlement(projectId, settlementMonthStart, monthlyData.getCompanyId());
        if (settlement == null) {
            settlement = new ProjectSettlement();
            settlement.setCompanyId(monthlyData.getCompanyId());
            settlement.setProjectId(projectId);
            settlement.setSettlementMonth(settlementMonthStart);
            settlement.setVersion(1);
            settlement.setCreatedBy(operatorId);
            settlement.setCreatedAt(LocalDateTime.now());
            settlement.setRemark("首次结算");
        } else {
            settlement.setVersion(settlement.getVersion() == null ? 1 : settlement.getVersion() + 1);
            settlement.setRemark("重新结算");
        }

        settlement.setCompanyId(monthlyData.getCompanyId());
        settlement.setTotalAmount(monthlyData.getGrandTotal());
        settlement.setStatus("approved");
        settlement.setUpdatedAt(LocalDateTime.now());

        if (settlement.getId() == null) {
            settlementMapper.insert(settlement);
        } else {
            settlementMapper.updateById(settlement);
        }

        monthlyData.setStatus("settled");
        monthlyData.setUpdatedAt(LocalDateTime.now());
        monthlyDataService.saveOrUpdate(monthlyData);

        log.info("项目结算成功: projectId={}, month={}, operatorId={}", projectId, settlementMonth, operatorId);
        return settlement;
    }

    private void validateOtherCostRatio(ProjectMonthlyData data) {
        BigDecimal otherCost = BigDecimal.valueOf(data.getOtherTotal() != null ? data.getOtherTotal() : 0);
        BigDecimal grandTotal = BigDecimal.valueOf(data.getGrandTotal() != null ? data.getGrandTotal() : 0);
        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal ratio = otherCost.divide(grandTotal, 4, BigDecimal.ROUND_HALF_UP);
        if (ratio.compareTo(OTHER_COST_RATIO_LIMIT) > 0) {
            throw new RuntimeException(String.format("其他费用占比%.2f%%超过20%%限制", ratio.multiply(BigDecimal.valueOf(100))));
        }
    }

    private void validateOutsourcedCost(ProjectMonthlyData data) {
        try {
            JsonUtils.CostData parsed = JsonUtils.parseCostData(data.getCostData());
            List<Map<String, Object>> outsourcedItems = parsed.getItems("outsource");
            if (outsourcedItems == null || outsourcedItems.isEmpty()) {
                return;
            }

            for (Map<String, Object> item : outsourcedItems) {
                double originalAmount = ((Number) item.getOrDefault("original_amount", 0)).doubleValue();
                double actualAmount = ((Number) item.getOrDefault("amount", 0)).doubleValue();
                if (originalAmount <= 0) {
                    continue;
                }
                BigDecimal expected = BigDecimal.valueOf(originalAmount).multiply(OUTSOURCED_DISCOUNT);
                BigDecimal actual = BigDecimal.valueOf(actualAmount);
                BigDecimal diff = actual.subtract(expected).abs();
                if (diff.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                    throw new RuntimeException(
                            String.format("外包费用未按80%%折算：原始%.2f，折后%.2f，期望%.2f", originalAmount, actualAmount, expected.doubleValue()));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("外包费用校验失败: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectSettlement reSettle(Long projectId, LocalDate settlementMonth, Long operatorId) {
        log.info("用户 {} 重新打开项目结算，projectId={}，month={}", operatorId, projectId, settlementMonth);
        LocalDateTime settlementMonthStart = toStartOfMonth(settlementMonth);

        ProjectMonthlyData monthlyData = monthlyDataService.getProjectMonthlyData(projectId, settlementMonth);
        if (monthlyData == null) {
            throw new RuntimeException("月度数据不存在");
        }
        ProjectSettlement settlement = findSettlement(projectId, settlementMonthStart, monthlyData.getCompanyId());
        if (settlement == null) {
            throw new RuntimeException("结算记录不存在");
        }

        settlement.setCompanyId(monthlyData.getCompanyId());
        settlement.setStatus("re_settled");
        settlement.setRemark("重新打开结算，等待修改后再次提交");
        settlement.setUpdatedAt(LocalDateTime.now());
        settlementMapper.updateById(settlement);

        monthlyData.setStatus("draft");
        monthlyData.setUpdatedAt(LocalDateTime.now());
        monthlyDataService.saveOrUpdate(monthlyData);

        log.info("项目重新打开结算: projectId={}, month={}, operatorId={}", projectId, settlementMonth, operatorId);
        return settlement;
    }

    private ProjectSettlement findSettlement(Long projectId, LocalDateTime settlementMonthStart, Long companyId) {
        QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("company_id", companyId)
                .eq("settlement_month", settlementMonthStart)
                .last("limit 1");
        return settlementMapper.selectOne(wrapper);
    }
}

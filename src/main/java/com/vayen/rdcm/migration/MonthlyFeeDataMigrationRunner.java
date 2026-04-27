package com.vayen.rdcm.migration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.monthly-fee-migration.enabled", havingValue = "true")
public class MonthlyFeeDataMigrationRunner implements ApplicationRunner {

    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;

    @Override
    public void run(ApplicationArguments args) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("cost_data");
        List<ProjectMonthlyData> records = projectMonthlyDataMapper.selectList(wrapper);

        int touched = 0;
        int skipped = 0;
        int failed = 0;

        for (ProjectMonthlyData record : records) {
            try {
                String original = record.getCostData();
                String normalized = JsonUtils.normalizeCostDataJson(original);
                if (sameJson(original, normalized)) {
                    skipped++;
                    continue;
                }

                JsonUtils.CostData parsed = JsonUtils.parseCostData(normalized);
                record.setCostData(normalized);
                record.setLaborTotal(parsed.getCategoryTotal("labor"));
                record.setDirectMaterialTotal(parsed.getCategoryTotal("direct"));
                record.setDirectFuelTotal(0.0);
                record.setDirectRentalTotal(0.0);
                record.setDepreciationTotal(parsed.getCategoryTotal("deprec") + parsed.getCategoryTotal("long_deferred"));
                record.setAmortizationTotal(parsed.getCategoryTotal("intangible"));
                record.setDesignTotal(parsed.getCategoryTotal("design"));
                record.setCommissioningTotal(parsed.getCategoryTotal("equip"));
                record.setOutsourcedTotal(parsed.getCategoryTotal("outsource"));
                record.setOtherTotal(parsed.getCategoryTotal("other"));
                record.setGrandTotal(parsed.calculateTotal());
                record.setUpdatedAt(LocalDateTime.now());
                projectMonthlyDataMapper.updateById(record);
                touched++;
            } catch (Exception ex) {
                failed++;
                log.error("月度费用分类迁移失败，id={}, projectId={}, workMonth={}",
                        record.getId(), record.getProjectId(), record.getWorkMonth(), ex);
            }
        }

        log.info("月度费用分类迁移完成：总数={}，更新={}，跳过={}，失败={}", records.size(), touched, skipped, failed);
    }

    private boolean sameJson(String left, String right) {
        return Objects.equals(blankToEmpty(left).replaceAll("\\s+", ""), blankToEmpty(right).replaceAll("\\s+", ""));
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}

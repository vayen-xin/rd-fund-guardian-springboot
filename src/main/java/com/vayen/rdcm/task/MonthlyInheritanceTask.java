package com.vayen.rdcm.task;

import com.vayen.rdcm.service.ProjectMonthlyDataService;
import com.vayen.rdcm.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 月度数据自动继承任务
 * 每月1号 00:05 执行
 */
@Component
@Slf4j
public class MonthlyInheritanceTask {

    @Autowired
    private ProjectMonthlyDataService monthlyDataService;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * 每月1号 00:05 执行
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 5 0 1 * ?")
    public void inheritMonthlyData() {
        log.info("开始执行月度数据自动继承任务...");
        
        try {
            // 获取所有进行中项目
            List<Long> ongoingProjectIds = projectService.getOngoingProjectIds();
            
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            LocalDate currentMonth = LocalDate.now();
            
            for (Long projectId : ongoingProjectIds) {
                try {
                    monthlyDataService.inheritFromLastMonth(projectId, lastMonth, currentMonth, 0L);
                    log.debug("项目 {} 从 {} 继承数据到 {} 完成", projectId, lastMonth, currentMonth);
                } catch (Exception e) {
                    log.error("项目 {} 数据继承失败", projectId, e);
                }
            }
            
            log.info("月度数据自动继承完成，共处理 {} 个项目", ongoingProjectIds.size());
        } catch (Exception e) {
            log.error("月度数据自动继承任务执行失败", e);
        }
    }
}

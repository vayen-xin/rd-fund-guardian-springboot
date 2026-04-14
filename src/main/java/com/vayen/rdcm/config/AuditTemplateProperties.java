package com.vayen.rdcm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.audit-template")
public class AuditTemplateProperties {
    /**
     * 公司级研发工资明细表模板路径
     */
    private String companyWagePath = "docs/附件二、研发工资明细表.xls";

    /**
     * 项目级研发支出辅助账模板路径
     */
    private String projectLedgerPath = "docs/2.研发支出”辅助账RD01明细.xls";
}

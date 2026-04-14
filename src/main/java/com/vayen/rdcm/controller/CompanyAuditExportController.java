package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.audit.AuditLog;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.CompanyWageExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/audit-exports/company-wages")
@RequiredArgsConstructor
public class CompanyAuditExportController {

    private final CompanyWageExportService companyWageExportService;
    private final CurrentUserService currentUserService;

    /**
     * 导出公司级研发工资明细表。
     */
    @GetMapping("/workbook")
    @SaCheckPermission("project:view")
    @AuditLog(module = "审计导出", action = "导出公司工资明细表")
    public ResponseEntity<Resource> exportWorkbook(@RequestParam String startMonth,
                                                   @RequestParam String endMonth,
                                                   @RequestParam(required = false) Long companyId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Long targetCompanyId = companyId != null ? companyId : currentUser.getCompanyId();
        byte[] bytes = companyWageExportService.exportWorkbook(
                targetCompanyId,
                YearMonth.parse(startMonth),
                YearMonth.parse(endMonth),
                currentUser
        );
        String fileName = "附件二-研发工资明细表-" + startMonth + "-" + endMonth + ".xls";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(new ByteArrayResource(bytes));
    }
}

package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.audit.AuditLog;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.AuditExportService;
import com.vayen.rdcm.service.ProjectLedgerExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audit-exports")
@RequiredArgsConstructor
public class AuditExportController {

    private final AuditExportService auditExportService;
    private final ProjectLedgerExportService projectLedgerExportService;
    private final CurrentUserService currentUserService;

    /**
     * 导出项目年度审计 Excel。
     */
    @GetMapping("/workbook")
    @SaCheckPermission("project:view")
    @AuditLog(module = "审计导出", action = "导出审计Excel")
    public ResponseEntity<Resource> exportWorkbook(@PathVariable Long projectId, @RequestParam Integer year) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        byte[] bytes = auditExportService.exportAuditWorkbook(projectId, year, currentUser);
        String fileName = "附件二-研发工资明细表-" + year + ".xlsx";
        return buildDownloadResponse(bytes, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * 导出项目年度审计材料包。
     */
    @GetMapping("/package")
    @SaCheckPermission("project:view")
    @AuditLog(module = "审计导出", action = "导出审计材料包")
    public ResponseEntity<Resource> exportPackage(@PathVariable Long projectId, @RequestParam Integer year) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        byte[] bytes = auditExportService.exportAuditPackage(projectId, year, currentUser);
        String fileName = "审计材料包-" + year + ".zip";
        return buildDownloadResponse(bytes, fileName, "application/zip");
    }

    /**
     * 导出项目级研发支出辅助账。
     */
    @GetMapping("/ledger")
    @SaCheckPermission("project:view")
    @AuditLog(module = "审计导出", action = "导出研发支出辅助账")
    public ResponseEntity<Resource> exportLedger(@PathVariable Long projectId,
                                                 @RequestParam String startMonth,
                                                 @RequestParam String endMonth) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        byte[] bytes = projectLedgerExportService.exportLedger(
                projectId,
                YearMonth.parse(startMonth),
                YearMonth.parse(endMonth),
                currentUser
        );
        String fileName = "研发支出辅助账-" + startMonth + "-" + endMonth + ".xls";
        return buildDownloadResponse(bytes, fileName, "application/vnd.ms-excel");
    }

    private ResponseEntity<Resource> buildDownloadResponse(byte[] bytes, String fileName, String contentType) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(new ByteArrayResource(bytes));
    }
}

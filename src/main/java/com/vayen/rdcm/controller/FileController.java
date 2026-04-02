package com.vayen.rdcm.controller;

import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ExpenseVoucher;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ExpenseVoucherService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
@AllArgsConstructor
public class FileController {

    private final ExpenseVoucherService expenseVoucherService;
    private final CurrentUserService currentUserService;

    /**
     * 上传凭证文件
     */
    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestParam Long projectId,
                                             @RequestParam String yearMonth,
                                             @RequestParam String category,
                                             @RequestParam("file") MultipartFile file) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        ExpenseVoucher voucher = expenseVoucherService.upload(projectId, yearMonth, category, file, currentUser);
        return Result.success(FileUploadResponse.from(voucher));
    }

    /**
     * 下载或预览凭证文件
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        ExpenseVoucher voucher = expenseVoucherService.getById(fileId, currentUser);
        Resource resource = expenseVoucherService.loadAsResource(fileId, currentUser);
        String encodedFileName = URLEncoder.encode(voucher.getOriginalFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(voucher.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(voucher.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    /**
     * 删除凭证文件
     */
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        expenseVoucherService.delete(fileId, currentUserService.getCurrentUser());
        return Result.success();
    }

    @Data
    static class FileUploadResponse {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String category;
        private String yearMonth;

        static FileUploadResponse from(ExpenseVoucher voucher) {
            FileUploadResponse response = new FileUploadResponse();
            response.setId(voucher.getId());
            response.setFileName(voucher.getOriginalFileName());
            response.setFileUrl("/api/v1/files/" + voucher.getId());
            response.setCategory(voucher.getCategory());
            response.setYearMonth(voucher.getYearMonth());
            return response;
        }
    }
}

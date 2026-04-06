package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.config.AppUploadProperties;
import com.vayen.rdcm.entity.ExpenseVoucher;
import com.vayen.rdcm.mapper.ExpenseVoucherMapper;
import com.vayen.rdcm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExpenseVoucherService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExpenseVoucherMapper expenseVoucherMapper;
    private final AppUploadProperties uploadProperties;
    private final ProjectService projectService;

    /**
     * 上传凭证文件并保存数据库元数据。
     * 关键点：
     * 1. 先校验项目访问权限
     * 2. 按 company/project/month/category 生成目录
     * 3. 先落盘，再写数据库
     */
    public ExpenseVoucher upload(Long projectId, String yearMonth, String category, MultipartFile file, CurrentUser currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        projectService.assertProjectAccess(projectId, currentUser);

        String sanitizedCategory = sanitizeSegment(category);
        String sanitizedMonth = sanitizeSegment(yearMonth);
        String originalFileName = file.getOriginalFilename() == null ? "unknown" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedFileName = buildStoredFileName(projectId, sanitizedMonth, sanitizedCategory, originalFileName);
        String relativePath = Paths.get(
                "company-" + currentUser.getCompanyId(),
                "project-" + projectId,
                sanitizedMonth,
                sanitizedCategory,
                storedFileName
        ).toString();

        // 用配置里的根目录拼真实路径，这样本地、Docker、服务器都能切换。
        Path rootPath = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }

        ExpenseVoucher voucher = new ExpenseVoucher();
        voucher.setCompanyId(currentUser.getCompanyId());
        voucher.setProjectId(projectId);
        voucher.setYearMonth(sanitizedMonth);
        voucher.setCategory(sanitizedCategory);
        voucher.setOriginalFileName(originalFileName);
        voucher.setStoredFileName(storedFileName);
        voucher.setRelativePath(relativePath.replace('\\', '/'));
        voucher.setContentType(file.getContentType());
        voucher.setFileSize(file.getSize());
        voucher.setUploadedBy(currentUser.getId());
        voucher.setCreatedAt(LocalDateTime.now());
        expenseVoucherMapper.insert(voucher);
        return voucher;
    }

    /**
     * 查询文件元数据并做公司权限校验。
     */
    public ExpenseVoucher getById(Long fileId, CurrentUser currentUser) {
        ExpenseVoucher voucher = expenseVoucherMapper.selectById(fileId);
        if (voucher == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        if (!currentUser.isAdmin() && !voucher.getCompanyId().equals(currentUser.getCompanyId())) {
            throw new IllegalArgumentException("禁止访问其他公司的文件");
        }
        return voucher;
    }

    /**
     * 把数据库里的相对路径转换成可下载的 Resource。
     */
    public Resource loadAsResource(Long fileId, CurrentUser currentUser) {
        ExpenseVoucher voucher = getById(fileId, currentUser);
        Path path = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize().resolve(voucher.getRelativePath()).normalize();
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("文件不存在或已丢失");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("文件路径解析失败", e);
        }
    }

    /**
     * 删除文件时，同时删除磁盘文件和数据库记录。
     */
    public void delete(Long fileId, CurrentUser currentUser) {
        ExpenseVoucher voucher = getById(fileId, currentUser);
        Path path = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize().resolve(voucher.getRelativePath()).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("删除文件失败", e);
        }
        expenseVoucherMapper.deleteById(voucher.getId());
    }

    public ExpenseVoucher findByProjectMonthCategoryAndName(Long projectId, String yearMonth, String category, String fileName) {
        QueryWrapper<ExpenseVoucher> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("`year_month`", yearMonth)
                .eq("category", category)
                .eq("original_file_name", fileName);
        return expenseVoucherMapper.selectOne(wrapper);
    }

    private String buildStoredFileName(Long projectId, String yearMonth, String category, String originalFileName) {
        String cleanedOriginal = originalFileName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return projectId + "_" + yearMonth + "_" + category + "_" + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + "_" + cleanedOriginal;
    }

    /**
     * 路径片段安全化，避免非法字符进入目录结构。
     */
    private String sanitizeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("路径参数不能为空");
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\-_]", "_");
    }
}

package com.vayen.rdcm.controller;

import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.AttendanceDtos;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.AttendanceService;
import com.vayen.rdcm.service.impl.AttendanceServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;

    /**
     * 下载打卡导入模板。
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String month) {
        Resource resource = attendanceService.buildTemplate(
                currentUserService.getCurrentUser(),
                projectId,
                month == null || month.isBlank() ? null : AttendanceServiceImpl.parseMonth(month)
        );
        String fileName = URLEncoder.encode("打卡导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 查询打卡记录列表。
     */
    @GetMapping
    public Result<List<AttendanceDtos.AttendanceListItem>> list(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(attendanceService.list(currentUserService.getCurrentUser(), employeeId, name, projectCode, startDate, endDate));
    }

    /**
     * 按工号或姓名匹配员工信息。
     */
    @GetMapping("/lookup")
    public Result<AttendanceDtos.AttendanceLookupResponse> lookup(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String name) {
        return Result.success(attendanceService.lookup(currentUserService.getCurrentUser(), employeeId, name));
    }

    /**
     * 上传 Excel 并预解析打卡记录。
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AttendanceDtos.AttendanceImportPreviewResponse> preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam String month) {
        return Result.success(attendanceService.previewImport(currentUserService.getCurrentUser(), file, AttendanceServiceImpl.parseMonth(month)));
    }

    /**
     * 确认导入预解析后的打卡记录。
     */
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestBody AttendanceDtos.AttendanceImportConfirmRequest request) {
        attendanceService.confirmImport(currentUserService.getCurrentUser(), request.getRows());
        return Result.success();
    }

    /**
     * 手动新增打卡记录。
     */
    @PostMapping
    public Result<Void> create(@RequestBody AttendanceDtos.AttendanceSaveRequest request) {
        attendanceService.save(currentUserService.getCurrentUser(), request);
        return Result.success();
    }

    /**
     * 修改打卡记录。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AttendanceDtos.AttendanceSaveRequest request) {
        attendanceService.update(currentUserService.getCurrentUser(), id, request);
        return Result.success();
    }

    /**
     * 删除打卡记录。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attendanceService.delete(currentUserService.getCurrentUser(), id);
        return Result.success();
    }
}

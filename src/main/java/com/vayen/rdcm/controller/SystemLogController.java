package com.vayen.rdcm.controller;

import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.SystemLogPageResponse;
import com.vayen.rdcm.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/system-logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    /**
     * 分页查询系统操作日志。
     */
    @GetMapping
    public Result<SystemLogPageResponse> pageLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(systemLogService.pageLogs(page, size, operator, startDate, endDate));
    }
}

package com.vayen.rdcm.service;

import com.vayen.rdcm.dto.SystemLogPageResponse;

import java.time.LocalDate;

public interface SystemLogService {

    SystemLogPageResponse pageLogs(long page, long size, String operator, LocalDate startDate, LocalDate endDate);
}

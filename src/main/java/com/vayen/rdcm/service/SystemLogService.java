package com.vayen.rdcm.service;

import com.vayen.rdcm.dto.SystemLogPageResponse;
import com.vayen.rdcm.security.CurrentUser;

import java.time.LocalDate;

public interface SystemLogService {

    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_FAIL = "FAIL";
    String STATUS_DENIED = "DENIED";

    SystemLogPageResponse pageLogs(long page, long size, String operator, LocalDate startDate, LocalDate endDate);

    void record(String module, String action, String details);

    void record(CurrentUser currentUser, String module, String action, String details);

    void record(CurrentUser currentUser, String module, String action, String details, String status, String resultMessage);
}

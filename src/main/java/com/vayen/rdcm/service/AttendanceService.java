package com.vayen.rdcm.service;

import com.vayen.rdcm.dto.AttendanceDtos;
import com.vayen.rdcm.dto.PageResponse;
import com.vayen.rdcm.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface AttendanceService {

    PageResponse<AttendanceDtos.AttendanceListItem> list(CurrentUser currentUser, Integer page, Integer size, String employeeId, String name, String projectCode, LocalDate startDate, LocalDate endDate);

    AttendanceDtos.AttendanceLookupResponse lookup(CurrentUser currentUser, String employeeId, String name);

    AttendanceDtos.AttendanceImportPreviewResponse previewImport(CurrentUser currentUser, MultipartFile file, YearMonth month);

    void confirmImport(CurrentUser currentUser, List<AttendanceDtos.AttendanceImportRow> rows);

    void save(CurrentUser currentUser, AttendanceDtos.AttendanceSaveRequest request);

    void update(CurrentUser currentUser, Long id, AttendanceDtos.AttendanceSaveRequest request);

    void delete(CurrentUser currentUser, Long id);

    Resource buildTemplate(CurrentUser currentUser, Long projectId, YearMonth month);
}

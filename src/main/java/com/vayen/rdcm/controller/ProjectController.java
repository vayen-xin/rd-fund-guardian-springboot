package com.vayen.rdcm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.dto.ProjectDetailResponse;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ProjectService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@AllArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    /**
     * 分页查询项目列表
     */
    @GetMapping({"", "/get"})
    public Result<Page<Project>> getProjects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {

        CurrentUser currentUser = currentUserService.getCurrentUser();
        return Result.success(projectService.getProjects(currentUser, page, size, status, name));
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{id}")
    public Result<ProjectDetailResponse> getProject(@PathVariable Long id) {
        return Result.success(projectService.getProjectDetail(id, currentUserService.getCurrentUser()));
    }

    /**
     * 获取项目可选员工下拉
     */
    @GetMapping("/employee-options")
    public Result<List<OptionItemResponse>> getEmployeeOptions(@RequestParam(required = false) String keyword) {
        return Result.success(projectService.getEmployeeOptions(currentUserService.getCurrentUser(), keyword));
    }

    /**
     * 获取项目可选设备下拉
     */
    @GetMapping("/device-options")
    public Result<List<OptionItemResponse>> getDeviceOptions(@RequestParam(required = false) String keyword) {
        return Result.success(projectService.getDeviceOptions(currentUserService.getCurrentUser(), keyword));
    }

    /**
     * 创建项目
     */
    @PostMapping({"", "/create"})
    public Result<Void> createProject(@RequestBody ProjectRequest request) {
        Project project = buildProject(request);
        projectService.createProject(project, request.getEmployeeIds(), request.getDeviceIds(), currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        projectService.updateProject(id, buildProject(request), request.getEmployeeIds(), request.getDeviceIds(), currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 更新项目状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        projectService.updateStatus(id, status, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 结束项目
     */
    @PutMapping("/{id}/end")
    public Result<Void> endProject(@PathVariable Long id) {
        projectService.updateStatus(id, "ended", currentUserService.getCurrentUser());
        return Result.success();
    }

    private Project buildProject(ProjectRequest request) {
        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setCode(request.getCode());
        project.setStartDate(request.getStartDate());
        project.setDescription(request.getDescription());
        project.setManagerName(request.getManagerName());
        project.setManagerPhone(request.getManagerPhone());
        return project;
    }
}

@Data
class ProjectRequest {
    private String projectName;
    private String code;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    private String description;
    private String managerName;
    private String managerPhone;
    private List<Long> employeeIds;
    private List<Long> deviceIds;
}

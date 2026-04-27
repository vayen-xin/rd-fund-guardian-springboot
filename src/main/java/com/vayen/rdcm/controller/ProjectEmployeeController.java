package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ProjectEmployeeService;
import com.vayen.rdcm.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目员工关联控制器
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/employees")
public class ProjectEmployeeController {

    private final ProjectEmployeeService employeeService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectEmployeeController(ProjectEmployeeService employeeService,
                                     ProjectService projectService,
                                     CurrentUserService currentUserService) {
        this.employeeService = employeeService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取项目下所有员工
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<List<ProjectEmployee>> getEmployees(@PathVariable Long projectId) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        return Result.success(employeeService.getByProjectId(projectId));
    }

    /**
     * 添加项目员工关联
     */
    @PostMapping
    @SaCheckPermission("project:edit")
    public Result<Void> addEmployee(@PathVariable Long projectId, @RequestBody ProjectEmployee employee) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        projectService.assertProjectAccess(projectId, currentUser);
        employee.setProjectId(projectId);
        employee.setCompanyId(currentUser.getCompanyId());
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employeeService.save(employee);
        return Result.success();
    }

    /**
     * 删除项目员工关联
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("project:edit")
    public Result<Void> removeEmployee(@PathVariable Long projectId, @PathVariable Long id) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        boolean removed = employeeService.removeByProjectAndId(projectId, id);
        if (!removed) {
            return Result.error(404, "关联记录不存在");
        }
        return Result.success();
    }
}

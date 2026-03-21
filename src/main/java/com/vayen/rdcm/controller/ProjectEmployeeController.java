package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.service.ProjectEmployeeService;
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

    public ProjectEmployeeController(ProjectEmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    /**
     * 获取项目下所有员工
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<List<ProjectEmployee>> getEmployees(@PathVariable Long projectId) {
        List<ProjectEmployee> list = employeeService.getByProjectId(projectId);
        return Result.success(list);
    }
    
    /**
     * 添加员工到项目
     */
    @PostMapping
    @SaCheckPermission("project:edit")
    public Result<Void> addEmployee(
            @PathVariable Long projectId,
            @RequestBody ProjectEmployee employee) {
        
        Long currentUserId = StpUtil.getLoginIdAsLong();
        employee.setProjectId(projectId);
        employee.setCreatedAt(LocalDateTime.now());
        employeeService.save(employee);
        
        return Result.success();
    }
    
    /**
     * 删除项目员工
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("project:edit")
    public Result<Void> removeEmployee(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        
        employeeService.removeById(id);
        return Result.success();
    }
}

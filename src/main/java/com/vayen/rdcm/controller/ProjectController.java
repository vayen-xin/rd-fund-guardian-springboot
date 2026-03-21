package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.mapper.ProjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目管理控制器（简化版）
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectMapper projectMapper;
    
    /**
     * 获取当前用户companyId（辅助方法）
     */
    private Long getCurrentCompanyId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Object companyIdObj = StpUtil.getSessionByLoginId(userId).get("companyId");
        return companyIdObj != null ? Long.parseLong(companyIdObj.toString()) : null;
    }
    
    /**
     * 分页查询项目列表
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<Page<Project>> getProjects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {
        
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        
        // 多租户：自动注入 companyId
        Long companyId = getCurrentCompanyId();
        if (companyId != null) {
            wrapper.eq("company_id", companyId);
        }
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (name != null && !name.isEmpty()) {
            wrapper.like("project_name", name);
        }
        
        Page<Project> projectPage = new Page<>(page, size);
        Page<Project> result = projectMapper.selectPage(projectPage, wrapper);
        
        return Result.success(result);
    }
    
    /**
     * 获取项目详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("project:view")
    public Result<Project> getProject(@PathVariable Long id) {
        Project project = projectMapper.selectById(id);
        return Result.success(project);
    }
    
    /**
     * 创建项目
     */
    @PostMapping
    @SaCheckPermission("project:create")
    public Result<Void> createProject(@RequestBody ProjectRequest request) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Long companyId = getCurrentCompanyId();
        
        Project project = new Project();
        project.setCompanyId(companyId);
        project.setProjectName(request.getProjectName());
        project.setCode(request.getCode());
        project.setStatus("pending");
        project.setStartDate(request.getStartDate());
        project.setDescription(request.getDescription());
        project.setManagerPhone(request.getManagerPhone());
        project.setCreatedBy(currentUserId);
        
        projectMapper.insert(project);
        return Result.success();
    }
    
    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    @SaCheckPermission("project:edit")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        
        project.setProjectName(request.getProjectName());
        project.setCode(request.getCode());
        project.setDescription(request.getDescription());
        project.setManagerPhone(request.getManagerPhone());
        project.setStartDate(request.getStartDate());
        
        projectMapper.updateById(project);
        return Result.success();
    }
    
    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("project:delete")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectMapper.deleteById(id);
        return Result.success();
    }
    
    /**
     * 更新项目状态
     */
    @PutMapping("/{id}/status")
    @SaCheckPermission("project:edit")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) { // pending/ongoing/ended/settled
        
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        
        project.setStatus(status);
        if ("ended".equals(status) && project.getEndDate() == null) {
            project.setEndDate(LocalDate.now());
        }
        
        projectMapper.updateById(project);
        return Result.success();
    }
}

/**
 * 项目创建/更新请求体
 */
@Data
class ProjectRequest {
    private String projectName;
    private String code;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    private String description;
    private String managerPhone;
}

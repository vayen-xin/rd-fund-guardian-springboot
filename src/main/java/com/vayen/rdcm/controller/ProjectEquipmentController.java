package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ProjectEquipmentService;
import com.vayen.rdcm.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目设备关联控制器
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/equipments")
public class ProjectEquipmentController {

    private final ProjectEquipmentService projectEquipmentService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectEquipmentController(ProjectEquipmentService projectEquipmentService,
                                      ProjectService projectService,
                                      CurrentUserService currentUserService) {
        this.projectEquipmentService = projectEquipmentService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取项目下所有设备
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<List<ProjectEquipment>> getEquipments(@PathVariable Long projectId) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        return Result.success(projectEquipmentService.getByProjectId(projectId));
    }

    /**
     * 添加项目设备关联
     */
    @PostMapping
    @SaCheckPermission("project:edit")
    public Result<Void> addEquipment(@PathVariable Long projectId, @RequestBody ProjectEquipment projectEquipment) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        projectEquipment.setProjectId(projectId);
        projectEquipment.setLinkedAt(projectEquipment.getLinkedAt() == null ? LocalDateTime.now() : projectEquipment.getLinkedAt());
        projectEquipmentService.save(projectEquipment);
        return Result.success();
    }

    /**
     * 删除项目设备关联
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("project:edit")
    public Result<Void> removeEquipment(@PathVariable Long projectId, @PathVariable Long id) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        projectEquipmentService.removeById(id);
        return Result.success();
    }
}

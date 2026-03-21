package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.service.ProjectEquipmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目设备关联控制器
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/equipments")
public class ProjectEquipmentController {

    private final ProjectEquipmentService projectEquipmentService;

    public ProjectEquipmentController(ProjectEquipmentService projectEquipmentService) {
        this.projectEquipmentService = projectEquipmentService;
    }
    
    /**
     * 获取项目下所有设备
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<List<ProjectEquipment>> getEquipments(@PathVariable Long projectId) {
        List<ProjectEquipment> list = projectEquipmentService.getByProjectId(projectId);
        return Result.success(list);
    }
    
    /**
     * 添加设备到项目
     */
    @PostMapping
    @SaCheckPermission("project:edit")
    public Result<Void> addEquipment(
            @PathVariable Long projectId,
            @RequestBody ProjectEquipment projectEquipment) {
        
        projectEquipment.setProjectId(projectId);
        projectEquipmentService.save(projectEquipment);
        return Result.success();
    }
    
    /**
     * 删除项目设备关联
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("project:edit")
    public Result<Void> removeEquipment(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        
        projectEquipmentService.removeById(id);
        return Result.success();
    }
}

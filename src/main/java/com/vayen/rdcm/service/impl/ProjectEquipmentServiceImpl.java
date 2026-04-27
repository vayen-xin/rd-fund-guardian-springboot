package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.mapper.DeviceMapper;
import com.vayen.rdcm.mapper.ProjectEquipmentMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.service.ProjectEquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectEquipmentServiceImpl extends ServiceImpl<ProjectEquipmentMapper, ProjectEquipment>
        implements ProjectEquipmentService {

    private final ProjectEquipmentMapper projectEquipmentMapper;
    private final ProjectMapper projectMapper;
    private final DeviceMapper deviceMapper;

    @Override
    public List<ProjectEquipment> getByProjectId(Long projectId) {
        Project project = requireProject(projectId);
        QueryWrapper<ProjectEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId).eq("company_id", project.getCompanyId());
        return this.list(wrapper);
    }

    /**
     * Rebuild project-equipment relations by device ids.
     */
    @Override
    public void replaceProjectEquipments(Long projectId, List<Long> deviceIds) {
        Project project = requireProject(projectId);
        QueryWrapper<ProjectEquipment> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("project_id", projectId).eq("company_id", project.getCompanyId());
        projectEquipmentMapper.delete(deleteWrapper);
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        for (Long deviceId : deviceIds) {
            validateDeviceBelongsToProjectCompany(project, deviceId);
            ProjectEquipment relation = new ProjectEquipment();
            relation.setCompanyId(project.getCompanyId());
            relation.setProjectId(projectId);
            relation.setDeviceId(deviceId);
            relation.setLinkedAt(LocalDateTime.now());
            projectEquipmentMapper.insert(relation);
        }
    }

    @Override
    public boolean save(ProjectEquipment entity) {
        if (entity.getLinkedAt() == null) {
            entity.setLinkedAt(LocalDateTime.now());
        }
        Project project = requireProject(entity.getProjectId());
        validateDeviceBelongsToProjectCompany(project, entity.getDeviceId());
        entity.setCompanyId(project.getCompanyId());
        return super.save(entity);
    }

    @Override
    public boolean removeByProjectAndId(Long projectId, Long relationId) {
        Project project = requireProject(projectId);
        QueryWrapper<ProjectEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("id", relationId)
                .eq("project_id", projectId)
                .eq("company_id", project.getCompanyId());
        return projectEquipmentMapper.delete(wrapper) > 0;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return project;
    }

    private void validateDeviceBelongsToProjectCompany(Project project, Long deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.eq("id", deviceId).eq("company_id", project.getCompanyId());
        Device device = deviceMapper.selectOne(wrapper);
        if (device == null) {
            throw new IllegalArgumentException("设备不属于当前项目所属公司");
        }
    }
}

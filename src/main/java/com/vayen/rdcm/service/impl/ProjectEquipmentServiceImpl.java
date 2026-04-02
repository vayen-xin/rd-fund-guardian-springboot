package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.mapper.ProjectEquipmentMapper;
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

    @Override
    public List<ProjectEquipment> getByProjectId(Long projectId) {
        QueryWrapper<ProjectEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        return this.list(wrapper);
    }

    /**
     * 按设备 ID 列表重建项目设备关联。
     */
    public void replaceProjectEquipments(Long projectId, List<Long> deviceIds) {
        QueryWrapper<ProjectEquipment> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("project_id", projectId);
        projectEquipmentMapper.delete(deleteWrapper);
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        for (Long deviceId : deviceIds) {
            ProjectEquipment relation = new ProjectEquipment();
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
        return super.save(entity);
    }
}

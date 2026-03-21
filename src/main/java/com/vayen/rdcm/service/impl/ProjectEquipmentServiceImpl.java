package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.mapper.ProjectEquipmentMapper;
import com.vayen.rdcm.service.ProjectEquipmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目设备关联服务实现
 */
@Service
public class ProjectEquipmentServiceImpl extends ServiceImpl<ProjectEquipmentMapper, ProjectEquipment> 
    implements ProjectEquipmentService {
    
    @Override
    public List<ProjectEquipment> getByProjectId(Long projectId) {
        QueryWrapper<ProjectEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        return this.list(wrapper);
    }
    
    @Override
    public boolean save(ProjectEquipment entity) {
        if (entity.getLinkedAt() == null) {
            entity.setLinkedAt(LocalDateTime.now());
        }
        return super.save(entity);
    }
}

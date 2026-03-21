package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vayen.rdcm.entity.ProjectEquipment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目设备关联服务
 */
@Service
public interface ProjectEquipmentService extends IService<ProjectEquipment> {
    
    /**
     * 获取项目下所有设备关联
     */
    List<ProjectEquipment> getByProjectId(Long projectId);
}

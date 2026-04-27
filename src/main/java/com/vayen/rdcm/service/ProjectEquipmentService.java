package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vayen.rdcm.entity.ProjectEquipment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProjectEquipmentService extends IService<ProjectEquipment> {

    /**
     * 获取项目下所有设备关联
     */
    List<ProjectEquipment> getByProjectId(Long projectId);

    /**
     * 按设备 ID 列表重建项目设备关联
     */
    void replaceProjectEquipments(Long projectId, List<Long> deviceIds);

    /**
     * 仅删除当前项目下的设备关联，避免按主键直接删除造成越权操作。
     */
    boolean removeByProjectAndId(Long projectId, Long relationId);
}

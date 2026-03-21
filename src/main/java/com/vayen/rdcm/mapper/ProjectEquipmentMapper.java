package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.ProjectEquipment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目设备关联 Mapper 接口
 */
@Mapper
public interface ProjectEquipmentMapper extends BaseMapper<ProjectEquipment> {
    
    /**
     * 根据项目ID查询所有关联设备
     */
    List<ProjectEquipment> selectByProjectId(Long projectId);
}

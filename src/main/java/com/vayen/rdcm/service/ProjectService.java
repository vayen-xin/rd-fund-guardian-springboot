package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务
 */
@Service
public class ProjectService {

    @Autowired
    private ProjectMapper projectMapper;
    
    /**
     * 获取所有进行中项目的ID列表
     */
    public List<Long> getOngoingProjectIds() {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.select("id")
               .eq("status", "ongoing");
        List<Object> objs = projectMapper.selectObjs(wrapper);
        return objs.stream()
            .map(obj -> ((Number) obj).longValue())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查询项目
     */
    public Project getProjectById(Long projectId) {
        // 使用 MyBatis-Plus 提供的 selectById
        return projectMapper.selectById(projectId); // 虽然 ProjectMapper extends BaseMapper，但有自定义方法。BaseMapper有selectById
    }
}

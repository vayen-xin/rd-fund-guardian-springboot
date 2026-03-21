package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.mapper.ProjectEmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目员工服务
 */
@Service
public class ProjectEmployeeService {

    @Autowired
    private ProjectEmployeeMapper employeeMapper;
    
    /**
     * 查询项目下所有员工
     */
    public List<ProjectEmployee> getByProjectId(Long projectId) {
        QueryWrapper<ProjectEmployee> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
               .orderByAsc("employee_name");
        return employeeMapper.selectList(wrapper);
    }
    
    /**
     * 添加员工
     */
    public void save(ProjectEmployee employee) {
        employeeMapper.insert(employee);
    }
    
    /**
     * 删除员工
     */
    public boolean removeById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }
}

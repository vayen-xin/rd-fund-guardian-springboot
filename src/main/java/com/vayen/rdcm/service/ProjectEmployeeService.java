package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.mapper.ProjectEmployeeMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectEmployeeService {

    private final ProjectEmployeeMapper employeeMapper;
    private final EmployeeMapper baseEmployeeMapper;
    private final ProjectMapper projectMapper;

    /**
     * 查询项目下所有员工
     */
    public List<ProjectEmployee> getByProjectId(Long projectId) {
        Project project = requireProject(projectId);
        QueryWrapper<ProjectEmployee> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("company_id", project.getCompanyId())
                .orderByAsc("employee_name");
        return employeeMapper.selectList(wrapper);
    }

    /**
     * 向项目添加单个员工关联
     */
    public void save(ProjectEmployee employee) {
        employee.setCreatedAt(employee.getCreatedAt() == null ? LocalDateTime.now() : employee.getCreatedAt());
        employeeMapper.insert(employee);
    }

    /**
     * 按员工 ID 列表重建项目员工关联。
     * 创建项目和更新项目时都复用这里，避免关联数据散在 controller 里处理。
     */
    public void replaceProjectEmployees(Long projectId, Long companyId, List<Long> employeeIds) {
        QueryWrapper<ProjectEmployee> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("project_id", projectId).eq("company_id", companyId);
        employeeMapper.delete(deleteWrapper);
        if (employeeIds == null || employeeIds.isEmpty()) {
            return;
        }

        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.in("id", employeeIds)
                .eq("company_id", companyId);
        List<Employee> employees = baseEmployeeMapper.selectList(wrapper);

        for (Employee employee : employees) {
            ProjectEmployee relation = new ProjectEmployee();
            relation.setCompanyId(companyId);
            relation.setProjectId(projectId);
            relation.setEmployeeId(employee.getId());
            relation.setEmployeeName(employee.getName());
            relation.setEmployeeType("formal");
            relation.setCoefficient(0.7);
            relation.setPhone(employee.getPhone());
            relation.setEmail(employee.getEmail());
            relation.setDepartment(employee.getDepartment());
            relation.setCreatedAt(LocalDateTime.now());
            relation.setUpdatedAt(LocalDateTime.now());
            employeeMapper.insert(relation);
        }
    }

    /**
     * 删除项目员工关联
     */
    public boolean removeById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }

    /**
     * 仅删除当前项目下的员工关联，避免按主键直接删除造成越权操作。
     */
    public boolean removeByProjectAndId(Long projectId, Long id) {
        Project project = requireProject(projectId);
        QueryWrapper<ProjectEmployee> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id)
                .eq("project_id", projectId)
                .eq("company_id", project.getCompanyId());
        return employeeMapper.delete(wrapper) > 0;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return project;
    }
}

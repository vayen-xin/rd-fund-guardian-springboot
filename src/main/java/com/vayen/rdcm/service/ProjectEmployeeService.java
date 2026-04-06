package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.mapper.ProjectEmployeeMapper;
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
        employeeMapper.deleteByProjectId(projectId);
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
}

package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Override
    public List<Employee> getEmployeesByCompanyId(Long companyId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("company_id", companyId)
                .orderByDesc("created_at");
        return this.list(wrapper);
    }

    @Override
    public Employee getByEmployeeId(String employeeId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId);
        return this.getOne(wrapper);
    }

    @Override
    public Employee getEmployeeById(Long id, Long companyId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if (companyId != null) {
            wrapper.eq("company_id", companyId);
        }
        Employee employee = this.getOne(wrapper);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        return employee;
    }

    @Override
    public void createEmployee(Employee employee, CurrentUser currentUser) {
        employee.setId(null);
        if (!currentUser.isAdmin()) {
            employee.setCompanyId(currentUser.getCompanyId());
        }
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        this.save(employee);
    }

    @Override
    public void updateEmployee(Long id, Employee employee, CurrentUser currentUser) {
        Employee existing = getEmployeeById(id, currentUser.isAdmin() ? null : currentUser.getCompanyId());
        existing.setEmployeeId(employee.getEmployeeId());
        existing.setName(employee.getName());
        existing.setGender(employee.getGender());
        existing.setPhone(employee.getPhone());
        existing.setEmail(employee.getEmail());
        existing.setDepartment(employee.getDepartment());
        existing.setPosition(employee.getPosition());
        existing.setEntryDate(employee.getEntryDate());
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
    }

    @Override
    public void deleteEmployee(Long id, CurrentUser currentUser) {
        Employee existing = getEmployeeById(id, currentUser.isAdmin() ? null : currentUser.getCompanyId());
        this.removeById(existing.getId());
    }

    @Override
    public List<OptionItemResponse> getEmployeeOptions(CurrentUser currentUser, String keyword) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("employee_id", keyword).or().like("name", keyword).or().like("department", keyword));
        }
        wrapper.orderByAsc("name");
        return this.list(wrapper).stream()
                .map(item -> new OptionItemResponse(item.getId(), item.getEmployeeId(), item.getName(), item.getDepartment()))
                .toList();
    }
}

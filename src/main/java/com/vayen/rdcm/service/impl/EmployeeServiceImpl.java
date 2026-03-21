package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 员工服务实现
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> 
    implements EmployeeService {
    
    @Override
    public List<Employee> getEmployeesByCompanyId(Long companyId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("company_id", companyId)
               .orderByDesc("create_time");
        return this.list(wrapper);
    }
    
    @Override
    public Employee getByEmployeeId(String employeeId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId);
        return this.getOne(wrapper);
    }
}

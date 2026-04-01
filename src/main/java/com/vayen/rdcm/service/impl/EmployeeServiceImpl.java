package com.vayen.rdcm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
    public Employee getEmployeeById(Long id , Long companyId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        Employee employee = this.getOne(wrapper.eq("id", id));
        if(Objects.equals(employee.getCompanyId(), companyId)){
            return employee;
        }else {
            throw new RuntimeException("id为"+StpUtil.getLoginId()+"的用户：禁止非法查询非本公司员工信息");
        }
    }
}

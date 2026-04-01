package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vayen.rdcm.entity.Employee;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 员工服务
 */
@Service
public interface EmployeeService extends IService<Employee> {
    
    /**
     * 根据公司ID查询员工列表
     */
    List<Employee> getEmployeesByCompanyId(Long companyId);
    
    /**
     * 根据员工编号查询员工
     */
    Employee getByEmployeeId(String employeeId);

    Employee getEmployeeById(Long id , Long companyId);
}

package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理控制器
 */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    /**
     * 获取所有员工列表（可按公司筛选）
     */
    @GetMapping
    @SaCheckPermission("employee:view")
    public Result<List<Employee>> getEmployees(@RequestParam(required = false) Long companyId) {
        List<Employee> list;
        if (companyId != null) {
            list = employeeService.getEmployeesByCompanyId(companyId);
        } else {
            list = employeeService.list();
        }
        return Result.success(list);
    }
    
    /**
     * 获取员工详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("employee:view")
    public Result<Employee> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
    
    /**
     * 创建员工
     */
    @PostMapping
    @SaCheckPermission("employee:create")
    public Result<Void> createEmployee(@RequestBody Employee employee) {
        employeeService.save(employee);
        return Result.success();
    }
    
    /**
     * 更新员工
     */
    @PutMapping("/{id}")
    @SaCheckPermission("employee:edit")
    public Result<Void> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        employeeService.updateById(employee);
        return Result.success();
    }
    
    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("employee:delete")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.removeById(id);
        return Result.success();
    }
}

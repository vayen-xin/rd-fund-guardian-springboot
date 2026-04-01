package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
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

    // 获取companyId辅助方法
    private Long getCompanyId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Object companyIdObj = StpUtil.getSessionByLoginId(userId).get("companyId");
        return companyIdObj != null ? Long.parseLong(companyIdObj.toString()) : null;
    }
    
    /**
     * 获取所有员工列表（可按公司筛选）
     */
    @GetMapping("/getEmployee")
    public Result<List<Employee>> getEmployees() {
        Long companyId = getCompanyId();
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
    public Result<Employee> getEmployee(@PathVariable Long id) {
        Long companyId = getCompanyId();
        Employee employee = employeeService.getEmployeeById(id , companyId);
        //Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
    
    /**
     * 创建员工
     */
    @PostMapping("/createEmployee")
    public Result<Void> createEmployee(@RequestBody Employee employee) {
        employeeService.save(employee);
        return Result.success();
    }
    
    /**
     * 更新员工
     */
    @PutMapping("/{id}")
    public Result<Void> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        employeeService.updateById(employee);
        return Result.success();
    }
    
    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.removeById(id);
        return Result.success();
    }
}

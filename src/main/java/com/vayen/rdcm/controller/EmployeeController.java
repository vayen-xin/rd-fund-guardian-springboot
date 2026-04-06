package com.vayen.rdcm.controller;

import com.vayen.rdcm.audit.AuditLog;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理接口。
 */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    public EmployeeController(EmployeeService employeeService, CurrentUserService currentUserService) {
        this.employeeService = employeeService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取员工列表
     */
    @GetMapping
    public Result<List<Employee>> getEmployees() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<Employee> list = currentUser.isAdmin()
                ? employeeService.list()
                : employeeService.getEmployeesByCompanyId(currentUser.getCompanyId());
        return Result.success(list);
    }

    /**
     * 获取员工详情
     */
    @GetMapping("/{id}")
    public Result<Employee> getEmployee(@PathVariable Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Long companyId = currentUser.isAdmin() ? null : currentUser.getCompanyId();
        return Result.success(employeeService.getEmployeeById(id, companyId));
    }

    /**
     * 获取员工下拉选项
     */
    @GetMapping("/options")
    public Result<List<OptionItemResponse>> getEmployeeOptions(@RequestParam(required = false) String keyword) {
        return Result.success(employeeService.getEmployeeOptions(currentUserService.getCurrentUser(), keyword));
    }

    /**
     * 创建员工
     */
    @PostMapping
    @AuditLog(module = "员工管理", action = "创建员工")
    public Result<Void> createEmployee(@RequestBody Employee employee) {
        employeeService.createEmployee(employee, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 更新员工
     */
    @PutMapping("/{id}")
    @AuditLog(module = "员工管理", action = "更新员工")
    public Result<Void> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        employeeService.updateEmployee(id, employee, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "员工管理", action = "删除员工")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id, currentUserService.getCurrentUser());
        return Result.success();
    }
}

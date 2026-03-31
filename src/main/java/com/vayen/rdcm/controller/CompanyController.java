package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.Company;
import com.vayen.rdcm.service.CompanyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司管理控制器
 */
@RestController
@RequestMapping("/api/v1/companies")
@SaCheckRole("admin")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }
    
    /**
     * 获取所有公司
     */
    @GetMapping("/getAllCompanies")
    public Result<List<Company>> getAllCompanies() {
        List<Company> list = companyService.getAllCompanies();
        return Result.success(list);
    }
    
    /**
     * 获取公司详情
     */
    // @SaCheckRole("admin")
    @GetMapping("/{id}")
    public Result<Company> getCompany(@PathVariable Long id) {
        Company company = companyService.getById(id);
        return Result.success(company);
    }
    
    /**
     * 创建公司
     */
    @PostMapping("/creatCompany")
    public Result<Void> createCompany(@RequestBody Company company) {
        company.setId(null);
        companyService.createCompany(company);
        return Result.success();
    }
    
    /**
     * 更新公司
     */
    @PutMapping("/{id}")
    public Result<Void> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        company.setId(id);
        companyService.updateCompany(company);
        return Result.success();
    }
    
    /**
     * 删除公司
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCompany(@PathVariable Long id) {
        companyService.removeCompany(id);
        return Result.success();
    }
}

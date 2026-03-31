package com.vayen.rdcm.service;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Company;
import com.vayen.rdcm.mapper.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公司服务
 */
@Service
@RequiredArgsConstructor
@SaCheckRole("admin")
public class CompanyService {


    private final CompanyMapper companyMapper;
    
    /**
     * 查询所有公司
     */
    public List<Company> getAllCompanies() {

        return companyMapper.selectList(new QueryWrapper<>());
    }
    
    /**
     * 根据ID查询公司
     */
    public Company getById(Long id) {
        return companyMapper.selectById(id);
    }
    
    /**
     * 创建公司
     */
    public void createCompany(Company company) {
        companyMapper.insert(company);
    }
    
    /**
     * 更新公司
     */
    public void updateCompany(Company company) {
        companyMapper.updateById(company);
    }
    
    /**
     * 删除公司
     */
    public boolean removeCompany(Long id) {
        return companyMapper.deleteById(id) > 0;
    }
}

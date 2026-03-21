package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工 Mapper 接口
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    
    /**
     * 根据公司ID查询员工列表
     */
    List<Employee> selectByCompanyId(@Param("companyId") Long companyId);
    
    /**
     * 根据员工编号查询
     */
    Employee findByEmployeeId(@Param("employeeId") String employeeId);
}


package com.vayen.rdcm.mapper;

import com.vayen.rdcm.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工 Mapper 接口
 */
@Mapper
public interface EmployeeMapper {
    
    /**
     * 查询所有启用的员工
     */
    List<Employee> findAllActive();
    
    /**
     * 按类型查询员工
     */
    List<Employee> findByType(@Param("type") String type);
    
    /**
     * 根据 ID 查询员工
     */
    Employee findById(@Param("id") Long id);
    
    /**
     * 插入员工
     */
    int insert(Employee employee);
    
    /**
     * 更新员工
     */
    int update(Employee employee);
    
    /**
     * 删除员工（逻辑删除）
     */
    int delete(@Param("id") Long id);
}

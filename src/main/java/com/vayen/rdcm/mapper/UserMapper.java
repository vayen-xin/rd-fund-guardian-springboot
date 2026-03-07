package com.vayen.rdcm.mapper;

import com.vayen.rdcm.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查询用户
     */
    UserAccount findByUsername(@Param("username") String username);
    
    /**
     * 根据 ID 查询用户
     */
    UserAccount findById(@Param("id") Long id);
    
    /**
     * 插入用户
     */
    int insert(UserAccount user);
    
    /**
     * 更新用户
     */
    int update(UserAccount user);
}

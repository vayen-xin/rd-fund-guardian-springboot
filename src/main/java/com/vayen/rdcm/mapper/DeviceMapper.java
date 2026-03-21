package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备 Mapper（扩展方法）
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
    
    /**
     * 查询公司的所有设备
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM device WHERE company_id = #{companyId} ORDER BY device_name")
    List<Device> findByCompanyId(@Param("companyId") Long companyId);
}

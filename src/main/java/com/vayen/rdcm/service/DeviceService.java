package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备服务
 */
@Service
public class DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;
    
    /**
     * 获取所有设备（可筛选公司）
     */
    public List<Device> getAllDevices(Long companyId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        if (companyId != null) {
            wrapper.eq("company_id", companyId);
        }
        wrapper.orderByAsc("device_name");
        return deviceMapper.selectList(wrapper);
    }
    
    /**
     * 创建设备
     */
    public void createDevice(Device device) {
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.insert(device);
    }
    
    /**
     * 更新设备
     */
    public void updateDevice(Device device) {
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
    }
    
    /**
     * 删除设备
     */
    public boolean removeDevice(Long id) {
        return deviceMapper.deleteById(id) > 0;
    }
}

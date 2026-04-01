package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

    public void createDevice(Device device) {
        // 处理空字符串 ---
        if (device.getStatus() != null && device.getStatus().trim().isEmpty()) {
            device.setStatus("normal"); // 设置为默认值
        }
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.insert(device);
    }
    
    /**
     * 更新设备
     */
    public void updateDevice(Device device) {
        // 使用 UpdateWrapper 构建动态更新条件
        UpdateWrapper<Device> updateWrapper = new UpdateWrapper<>();

        // 通过 ID 确定更新的目标记录
        updateWrapper.eq("id", device.getId());

        // --- 动态设置需要更新的字段 ---
        // 只有当字段不为 null 且不为空字符串时，才添加到更新条件中

        // 设备名称 (VARCHAR, 不应为空)
        if (device.getDeviceName() != null && !device.getDeviceName().trim().isEmpty()) {
            updateWrapper.set("device_name", device.getDeviceName().trim());
        }

        // 设备型号 (VARCHAR)
        if (device.getModel() != null && !device.getModel().trim().isEmpty()) {
            updateWrapper.set("model", device.getModel().trim());
        }

        // 设备规格 (TEXT)
        if (device.getSpecification() != null && !device.getSpecification().trim().isEmpty()) {
            updateWrapper.set("specification", device.getSpecification().trim());
        }

        // 购买日期 (DATE)
        if (device.getPurchaseDate() != null) { // Date 类型一般不区分空字符串
            updateWrapper.set("purchase_date", device.getPurchaseDate());
        }

        // 购买价格 (DECIMAL)
        if (device.getPurchasePrice() != null) { // 数字类型不区分空字符串
            updateWrapper.set("purchase_price", device.getPurchasePrice());
        }

        // 每日折旧 (DECIMAL)
        if (device.getDailyDepreciation() != null) {
            updateWrapper.set("daily_depreciation", device.getDailyDepreciation());
        }

        // 每月租赁单价 (DECIMAL)
        if (device.getMonthlyRental() != null) {
            updateWrapper.set("monthly_rental", device.getMonthlyRental());
        }

        // 设备状态 (ENUM, 需要特别处理空字符串)
        if (device.getStatus() != null && !device.getStatus().trim().isEmpty()) {
            updateWrapper.set("status", device.getStatus().trim());
        }

        // 备注 (TEXT)
        if (device.getNotes() != null && !device.getNotes().trim().isEmpty()) {
            updateWrapper.set("notes", device.getNotes().trim());
        }

        // --- 设置更新时间 ---
        updateWrapper.set("updated_at", LocalDateTime.now());

        // --- 执行更新 ---
        // 第一个参数为 null，因为更新条件和字段都由 UpdateWrapper 定义
        deviceMapper.update(null, updateWrapper);
    }
    
    /**
     * 删除设备
     */
    public boolean removeDevice(Long id) {
        return deviceMapper.deleteById(id) > 0;
    }
}

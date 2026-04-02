package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    public List<Device> getAllDevices(Long companyId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        if (companyId != null) {
            wrapper.eq("company_id", companyId);
        }
        wrapper.orderByAsc("device_name");
        return deviceMapper.selectList(wrapper);
    }

    public List<OptionItemResponse> getDeviceOptions(CurrentUser currentUser, String keyword) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("device_name", keyword).or().like("model", keyword));
        }
        wrapper.orderByAsc("device_name");
        return deviceMapper.selectList(wrapper).stream()
                .map(item -> new OptionItemResponse(item.getId(), String.valueOf(item.getId()), item.getDeviceName(), item.getModel()))
                .toList();
    }

    public Device getDeviceById(Long id, Long companyId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if (companyId != null) {
            wrapper.eq("company_id", companyId);
        }
        Device device = deviceMapper.selectOne(wrapper);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return device;
    }

    public void createDevice(Device device, CurrentUser currentUser) {
        if (!currentUser.isAdmin()) {
            device.setCompanyId(currentUser.getCompanyId());
        }
        if (device.getStatus() == null || device.getStatus().trim().isEmpty()) {
            device.setStatus("normal");
        }
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.insert(device);
    }

    public void updateDevice(Long id, Device device, CurrentUser currentUser) {
        Device existing = getDeviceById(id, currentUser.isAdmin() ? null : currentUser.getCompanyId());
        UpdateWrapper<Device> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", existing.getId());

        if (device.getDeviceName() != null && !device.getDeviceName().trim().isEmpty()) {
            updateWrapper.set("device_name", device.getDeviceName().trim());
        }
        if (device.getModel() != null && !device.getModel().trim().isEmpty()) {
            updateWrapper.set("model", device.getModel().trim());
        }
        if (device.getSpecification() != null && !device.getSpecification().trim().isEmpty()) {
            updateWrapper.set("specification", device.getSpecification().trim());
        }
        if (device.getPurchaseDate() != null) {
            updateWrapper.set("purchase_date", device.getPurchaseDate());
        }
        if (device.getPurchasePrice() != null) {
            updateWrapper.set("purchase_price", device.getPurchasePrice());
        }
        if (device.getDailyDepreciation() != null) {
            updateWrapper.set("daily_depreciation", device.getDailyDepreciation());
        }
        if (device.getMonthlyRental() != null) {
            updateWrapper.set("monthly_rental", device.getMonthlyRental());
        }
        if (device.getStatus() != null && !device.getStatus().trim().isEmpty()) {
            updateWrapper.set("status", device.getStatus().trim());
        }
        if (device.getNotes() != null && !device.getNotes().trim().isEmpty()) {
            updateWrapper.set("notes", device.getNotes().trim());
        }
        updateWrapper.set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, updateWrapper);
    }

    public void removeDevice(Long id, CurrentUser currentUser) {
        Device existing = getDeviceById(id, currentUser.isAdmin() ? null : currentUser.getCompanyId());
        deviceMapper.deleteById(existing.getId());
    }
}

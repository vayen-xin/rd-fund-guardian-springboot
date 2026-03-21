package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.service.DeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理控制器
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }
    
    /**
     * 获取设备列表（可按公司筛选）
     */
    @GetMapping
    @SaCheckPermission("device:view")
    public Result<List<Device>> getDevices(@RequestParam(required = false) Long companyId) {
        List<Device> list = deviceService.getAllDevices(companyId);
        return Result.success(list);
    }
    
    /**
     * 创建设备
     */
    @PostMapping
    @SaCheckPermission("device:create")
    public Result<Void> createDevice(@RequestBody Device device) {
        deviceService.createDevice(device);
        return Result.success();
    }
    
    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    @SaCheckPermission("device:edit")
    public Result<Void> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        deviceService.updateDevice(device);
        return Result.success();
    }
    
    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("device:delete")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.removeDevice(id);
        return Result.success();
    }
}

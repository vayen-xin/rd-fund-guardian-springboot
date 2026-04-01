package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
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

    // 获取companyId辅助方法
    private Long getCompanyId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Object companyIdObj = StpUtil.getSessionByLoginId(userId).get("companyId");
        return companyIdObj != null ? Long.parseLong(companyIdObj.toString()) : null;
    }

    /**
     * 获取设备列表（可按公司筛选）
     */
    @GetMapping("/getDevices")
    public Result<List<Device>> getDevices() {
        Long companyId = getCompanyId();
        List<Device> list = deviceService.getAllDevices(companyId);
        return Result.success(list);
    }

    /**
     * 创建设备
     */
    @PostMapping("/createDevices")
    public Result<Void> createDevice(@RequestBody Device device) {
        device.setId(null);
        deviceService.createDevice(device);
        return Result.success();
    }
    
    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    public Result<Void> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        deviceService.updateDevice(device);
        return Result.success();
    }
    
    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.removeDevice(id);
        return Result.success();
    }
}

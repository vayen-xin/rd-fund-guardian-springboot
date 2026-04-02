package com.vayen.rdcm.controller;

import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.DeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final CurrentUserService currentUserService;

    public DeviceController(DeviceService deviceService, CurrentUserService currentUserService) {
        this.deviceService = deviceService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取设备列表
     */
    @GetMapping({"", "/getDevices"})
    public Result<List<Device>> getDevices() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Long companyId = currentUser.isAdmin() ? null : currentUser.getCompanyId();
        return Result.success(deviceService.getAllDevices(companyId));
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/{id}")
    public Result<Device> getDevice(@PathVariable Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Long companyId = currentUser.isAdmin() ? null : currentUser.getCompanyId();
        return Result.success(deviceService.getDeviceById(id, companyId));
    }

    /**
     * 获取设备下拉选项
     */
    @GetMapping("/options")
    public Result<List<OptionItemResponse>> getDeviceOptions(@RequestParam(required = false) String keyword) {
        return Result.success(deviceService.getDeviceOptions(currentUserService.getCurrentUser(), keyword));
    }

    /**
     * 创建设备
     */
    @PostMapping({"", "/createDevices"})
    public Result<Void> createDevice(@RequestBody Device device) {
        deviceService.createDevice(device, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    public Result<Void> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        deviceService.updateDevice(id, device, currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.removeDevice(id, currentUserService.getCurrentUser());
        return Result.success();
    }
}

package com.example.vkr.mapper;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceDto toDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setHostname(device.getHostname());
        dto.setIpAddress(device.getIpAddress());
        dto.setOs(device.getOs());
        dto.setStatus(device.getStatus());
        dto.setLastCheck(device.getLastCheck());
        if (device.getUser() != null) {
            dto.setUserId(device.getUser().getId());
        }
        return dto;
    }

    public Device toEntity(DeviceDto dto, User user) {
        Device device = new Device();
        device.setId(dto.getId());
        device.setHostname(dto.getHostname());
        device.setIpAddress(dto.getIpAddress());
        device.setOs(dto.getOs());
        device.setStatus(dto.getStatus());
        device.setLastCheck(dto.getLastCheck());
        device.setUser(user);
        return device;
    }
}
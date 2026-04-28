package com.example.vkr.mapper;

import com.example.vkr.dto.InterfaceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Interface;
import org.springframework.stereotype.Component;

@Component
public class InterfaceMapper {

    public InterfaceDto toDto(Interface entity) {
        InterfaceDto dto = new InterfaceDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setMacAddress(entity.getMacAddress());
        dto.setSpeed(entity.getSpeed());
        dto.setStatus(entity.getStatus());
        if (entity.getDevice() != null) {
            dto.setDeviceId(entity.getDevice().getId());
        }
        return dto;
    }

    public Interface toEntity(InterfaceDto dto, Device device) {
        Interface entity = new Interface();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setMacAddress(dto.getMacAddress());
        entity.setSpeed(dto.getSpeed());
        entity.setStatus(dto.getStatus());
        entity.setDevice(device);
        return entity;
    }
}
package com.example.vkr.service;

import com.example.vkr.dto.InterfaceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Interface;
import com.example.vkr.mapper.InterfaceMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.InterfaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterfaceService {

    private final InterfaceRepository interfaceRepository;
    private final DeviceRepository deviceRepository;
    private final InterfaceMapper interfaceMapper;

    public List<InterfaceDto> getAll() {
        return interfaceRepository.findAll()
                .stream()
                .map(interfaceMapper::toDto)
                .collect(Collectors.toList());
    }

    public InterfaceDto create(InterfaceDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        Interface entity = interfaceMapper.toEntity(dto, device);
        Interface saved = interfaceRepository.save(entity);
        return interfaceMapper.toDto(saved);
    }

    public InterfaceDto update(InterfaceDto dto) {
        Interface existing = interfaceRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Interface not found"));
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getMacAddress() != null) existing.setMacAddress(dto.getMacAddress());
        if (dto.getSpeed() != null) existing.setSpeed(dto.getSpeed());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        Interface updated = interfaceRepository.save(existing);
        return interfaceMapper.toDto(updated);
    }

    public List<InterfaceDto> getByDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return interfaceRepository.findByDevice(device)
                .stream()
                .map(interfaceMapper::toDto)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        interfaceRepository.deleteById(id);
    }
}
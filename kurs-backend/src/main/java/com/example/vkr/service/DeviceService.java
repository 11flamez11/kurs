package com.example.vkr.service;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.DeviceMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceMapper deviceMapper;

    public List<DeviceDto> getAll() {
        return deviceRepository.findAll()
                .stream()
                .map(deviceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<DeviceDto> getByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return deviceRepository.findByUser(user)
                .stream()
                .map(deviceMapper::toDto)
                .collect(Collectors.toList());
    }

    public DeviceDto getById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        return deviceMapper.toDto(device);
    }

    public DeviceDto create(DeviceDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device device = deviceMapper.toEntity(dto, user);
        device.setLastCheck(LocalDateTime.now());
        Device saved = deviceRepository.save(device);
        return deviceMapper.toDto(saved);
    }

    public DeviceDto update(DeviceDto dto) {
        Device existingDevice = deviceRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (dto.getHostname() != null) {
            existingDevice.setHostname(dto.getHostname());
        }
        if (dto.getIpAddress() != null) {
            existingDevice.setIpAddress(dto.getIpAddress());
        }
        if (dto.getOs() != null) {
            existingDevice.setOs(dto.getOs());
        }
        if (dto.getStatus() != null) {
            existingDevice.setStatus(dto.getStatus());
        }
        if (dto.getLastCheck() != null) {
            existingDevice.setLastCheck(dto.getLastCheck());
        }
        existingDevice.setLastCheck(LocalDateTime.now());
        Device updated = deviceRepository.save(existingDevice);
        return deviceMapper.toDto(updated);
    }


    public void delete(Long id) {
        deviceRepository.deleteById(id);
    }
}
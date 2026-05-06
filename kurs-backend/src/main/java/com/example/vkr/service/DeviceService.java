package com.example.vkr.service;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.DeviceMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private static final long OFFLINE_AFTER_SECONDS = 45L;

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceMapper deviceMapper;

    public List<DeviceDto> getAll() {
        List<Device> devices = deviceRepository.findAll();
        refreshStatuses(devices);
        return devices.stream()
                .sorted(Comparator.comparing(
                        Device::getLastCheck,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(deviceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<DeviceDto> getByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Device> devices = deviceRepository.findByUser(user);
        refreshStatuses(devices);
        return devices.stream()
                .sorted(Comparator.comparing(
                        Device::getLastCheck,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(deviceMapper::toDto)
                .collect(Collectors.toList());
    }

    public DeviceDto getById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        refreshStatuses(List.of(device));
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

    public DeviceDto registerCurrentMachine(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostname = localHost.getHostName();
            String ipAddress = localHost.getHostAddress();
            String os = System.getProperty("os.name") + " " + System.getProperty("os.version");

            Device device = deviceRepository.findByUserAndHostnameAndIpAddress(user, hostname, ipAddress)
                    .orElseGet(() -> Device.builder()
                            .user(user)
                            .hostname(hostname)
                            .ipAddress(ipAddress)
                            .build());

            device.setOs(os);
            device.setStatus("ONLINE");
            device.setLastCheck(LocalDateTime.now());

            Device saved = deviceRepository.save(device);
            return deviceMapper.toDto(saved);
        } catch (Exception e) {
            throw new RuntimeException("Unable to register current machine: " + e.getMessage(), e);
        }
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

    private void refreshStatuses(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (Device device : devices) {
            String computedStatus = computeStatus(device.getLastCheck(), now);
            if (device.getStatus() == null || !device.getStatus().equalsIgnoreCase(computedStatus)) {
                device.setStatus(computedStatus);
                changed = true;
            }
        }
        if (changed) {
            deviceRepository.saveAll(devices);
        }
    }

    private String computeStatus(LocalDateTime lastCheck, LocalDateTime now) {
        if (lastCheck == null) {
            return "OFFLINE";
        }
        return lastCheck.isAfter(now.minusSeconds(OFFLINE_AFTER_SECONDS)) ? "ONLINE" : "OFFLINE";
    }
}
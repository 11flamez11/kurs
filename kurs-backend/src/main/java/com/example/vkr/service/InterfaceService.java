package com.example.vkr.service;

import com.example.vkr.dto.InterfaceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Interface;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.InterfaceMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.InterfaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterfaceService {

    private final InterfaceRepository interfaceRepository;
    private final DeviceRepository deviceRepository;
    private final InterfaceMapper interfaceMapper;

    private static final Comparator<Interface> INTERFACE_SORT = Comparator
            .comparing((Interface i) -> i.getDevice() != null ? i.getDevice().getLastCheck() : null,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing((Interface i) -> !"UP".equalsIgnoreCase(i.getStatus()))
            .thenComparing(Interface::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    public List<InterfaceDto> getAll() {
        return interfaceRepository.findAll()
                .stream()
                .sorted(INTERFACE_SORT)
                .map(interfaceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<InterfaceDto> getAllByUser(User user) {
        return interfaceRepository.findByDeviceUser(user)
                .stream()
                .sorted(INTERFACE_SORT)
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

    public List<InterfaceDto> registerLocalInterfaces(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        SystemInfo systemInfo = new SystemInfo();
        List<NetworkIF> localInterfaces = systemInfo.getHardware().getNetworkIFs();

        java.util.ArrayList<InterfaceDto> result = new java.util.ArrayList<>();
        for (NetworkIF nif : localInterfaces) {
            nif.updateAttributes();
            String rawName = nif.getDisplayName() != null && !nif.getDisplayName().isBlank()
                    ? nif.getDisplayName()
                    : nif.getName();
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = truncate(rawName.trim(), 50);

            boolean isLoopback = name.toLowerCase().contains("loopback");
            if (isLoopback) {
                continue;
            }

            Interface entity = interfaceRepository.findByDeviceAndName(device, name)
                    .orElseGet(() -> Interface.builder().device(device).name(name).build());

            entity.setMacAddress(truncate(nif.getMacaddr(), 17));
            long speedBps = Math.max(0L, nif.getSpeed());
            int speedMbps = speedBps > 0 ? (int) Math.min(Integer.MAX_VALUE, speedBps / 1_000_000L) : 0;
            entity.setSpeed(speedMbps);
            boolean up = nif.getIfOperStatus() != null && "UP".equalsIgnoreCase(nif.getIfOperStatus().name());
            entity.setStatus(truncate(up ? "UP" : "DOWN", 10));

            Interface saved = interfaceRepository.save(entity);
            result.add(interfaceMapper.toDto(saved));
        }

        return result.stream()
                .sorted(Comparator
                        .comparing((InterfaceDto i) -> !"UP".equalsIgnoreCase(i.getStatus()))
                        .thenComparing(InterfaceDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
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
                .sorted(Comparator
                        .comparing((Interface i) -> !"UP".equalsIgnoreCase(i.getStatus()))
                        .thenComparing(Interface::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(interfaceMapper::toDto)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        interfaceRepository.deleteById(id);
    }
}
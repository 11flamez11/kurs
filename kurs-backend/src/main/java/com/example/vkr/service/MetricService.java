package com.example.vkr.service;

import com.example.vkr.dto.MetricDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Metric;
import com.example.vkr.mapper.MetricMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;
    private final MetricMapper metricMapper;

    public List<MetricDto> getAll() {
        return metricRepository.findAll()
                .stream()
                .map(metricMapper::toDto)
                .collect(Collectors.toList());
    }

    public MetricDto create(MetricDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }
        Metric metric = metricMapper.toEntity(dto, device);
        Metric saved = metricRepository.save(metric);
        return metricMapper.toDto(saved);
    }

    public List<MetricDto> getByDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return metricRepository.findByDevice(device)
                .stream()
                .map(metricMapper::toDto)
                .collect(Collectors.toList());
    }

    public MetricDto update(MetricDto dto) {
        Metric existing = metricRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        if (dto.getCpuLoad() != null) existing.setCpuLoad(dto.getCpuLoad());
        if (dto.getRamUsage() != null) existing.setRamUsage(dto.getRamUsage());
        if (dto.getDiskUsage() != null) existing.setDiskUsage(dto.getDiskUsage());
        if (dto.getUptime() != null) existing.setUptime(dto.getUptime());
        if (dto.getTimestamp() != null) existing.setTimestamp(dto.getTimestamp());

        Metric updated = metricRepository.save(existing);
        return metricMapper.toDto(updated);
    }

    public void delete(Long id) {
        metricRepository.deleteById(id);
    }
}
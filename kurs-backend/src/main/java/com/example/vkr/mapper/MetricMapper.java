package com.example.vkr.mapper;

import com.example.vkr.dto.MetricDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Metric;
import org.springframework.stereotype.Component;

@Component
public class MetricMapper {

    public MetricDto toDto(Metric metric) {
        MetricDto dto = new MetricDto();
        dto.setId(metric.getId());
        dto.setCpuLoad(metric.getCpuLoad());
        dto.setRamUsage(metric.getRamUsage());
        dto.setDiskUsage(metric.getDiskUsage());
        dto.setUploadMbps(metric.getUploadMbps());
        dto.setDownloadMbps(metric.getDownloadMbps());
        dto.setPingMs(metric.getPingMs());
        dto.setPacketLossPercent(metric.getPacketLossPercent());
        dto.setUptime(metric.getUptime());
        dto.setTimestamp(metric.getTimestamp());
        if (metric.getDevice() != null) {
            dto.setDeviceId(metric.getDevice().getId());
        }
        return dto;
    }

    public Metric toEntity(MetricDto dto, Device device) {
        Metric metric = new Metric();
        metric.setId(dto.getId());
        metric.setCpuLoad(dto.getCpuLoad());
        metric.setRamUsage(dto.getRamUsage());
        metric.setDiskUsage(dto.getDiskUsage());
        metric.setUploadMbps(dto.getUploadMbps());
        metric.setDownloadMbps(dto.getDownloadMbps());
        metric.setPingMs(dto.getPingMs());
        metric.setPacketLossPercent(dto.getPacketLossPercent());
        metric.setUptime(dto.getUptime());
        metric.setTimestamp(dto.getTimestamp());
        metric.setDevice(device);
        return metric;
    }
}
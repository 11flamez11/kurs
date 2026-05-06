package com.example.vkr.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MetricDto {
    private Long id;
    private Double cpuLoad;
    private Double ramUsage;
    private Double diskUsage;
    private Double uploadMbps;
    private Double downloadMbps;
    private Double pingMs;
    private Double packetLossPercent;
    private Long uptime;
    private LocalDateTime timestamp;
    private Long deviceId;
}
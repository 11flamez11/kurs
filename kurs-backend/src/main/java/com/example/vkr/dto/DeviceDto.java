package com.example.vkr.dto;

import lombok.*;

@Data
public class DeviceDto {

    private Long id;
    private String hostname;
    private String ipAddress;
    private String os;
    private String status;
    private Long userId;
    private java.time.LocalDateTime lastCheck;
}
package com.example.vkr.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceDto {

    private Long id;
    private String name;
    private String macAddress;
    private Integer speed;
    private String status;
    private Long deviceId;
}
package com.example.vkr.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;
    private String severity;
    private String description;
    private Boolean resolved;
    private LocalDateTime timestamp;
    private Long deviceId;
    private Long createdBy;
}
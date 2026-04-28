package com.example.vkr.mapper;

import com.example.vkr.dto.EventDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Event;
import com.example.vkr.entity.User;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setSeverity(event.getSeverity());
        dto.setDescription(event.getDescription());
        dto.setResolved(event.isResolved());
        dto.setTimestamp(event.getTimestamp());
        if (event.getDevice() != null) {
            dto.setDeviceId(event.getDevice().getId());
        }
        if (event.getCreatedBy() != null) {
            dto.setCreatedBy(event.getCreatedBy().getId());
        }
        return dto;
    }

    public Event toEntity(EventDto dto, Device device, User createdBy) {
        Event event = new Event();
        event.setId(dto.getId());
        event.setSeverity(dto.getSeverity());
        event.setDescription(dto.getDescription());
        event.setResolved(dto.getResolved());
        event.setTimestamp(dto.getTimestamp());
        event.setDevice(device);
        event.setCreatedBy(createdBy);
        return event;
    }
}
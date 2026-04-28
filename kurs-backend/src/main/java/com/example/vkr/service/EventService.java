package com.example.vkr.service;

import com.example.vkr.dto.EventDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Event;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.EventMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.EventRepository;
import com.example.vkr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    public List<EventDto> getAll() {
        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    public EventDto create(EventDto dto, String username) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }
        Event event = eventMapper.toEntity(dto, device, user);
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    public List<EventDto> getByDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return eventRepository.findByDevice(device)
                .stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    public EventDto update(EventDto dto) {
        Event existingEvent = eventRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (dto.getSeverity() != null) {
            existingEvent.setSeverity(dto.getSeverity());
        }
        if (dto.getDescription() != null) {
            existingEvent.setDescription(dto.getDescription());
        }
        if (dto.getResolved() != null) {
            existingEvent.setResolved(dto.getResolved());
        }
        if (dto.getTimestamp() != null) {
            existingEvent.setTimestamp(dto.getTimestamp());
        }
        Event updated = eventRepository.save(existingEvent);
        return eventMapper.toDto(updated);
    }
    public void delete(Long id) {
        eventRepository.deleteById(id);
    }
}
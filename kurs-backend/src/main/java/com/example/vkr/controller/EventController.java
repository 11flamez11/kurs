package com.example.vkr.controller;

import com.example.vkr.dto.EventDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Event;
import com.example.vkr.entity.User;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.EventRepository;
import com.example.vkr.repository.UserRepository;
import com.example.vkr.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final DeviceRepository deviceRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<EventDto> getAll(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? eventService.getAll() : eventService.getAllByUser(user);
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getByDevice(@PathVariable Long deviceId, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = device.getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(eventService.getByDevice(deviceId));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody EventDto dto,
            Authentication authentication) {
        if (dto.getDeviceId() == null) {
            return ResponseEntity.badRequest().body("Device ID is required");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        boolean isOwner = device.getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admin or device owner can create events");
        }
        try {
            EventDto created = eventService.create(dto, username);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error creating event: " + e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody EventDto dto,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))
                && (existingEvent.getCreatedBy() == null
                || !existingEvent.getCreatedBy().getId().equals(user.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admin or creator can update this event");
        }
        try {
            dto.setId(id);
            EventDto updated = eventService.update(dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating event: " + e.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            eventService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
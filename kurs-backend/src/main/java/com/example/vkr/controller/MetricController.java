package com.example.vkr.controller;

import com.example.vkr.dto.MetricDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import com.example.vkr.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<MetricDto> getAll() {
        return metricService.getAll();
    }

    @GetMapping("/device/{deviceId}")
    public List<MetricDto> getByDevice(@PathVariable Long deviceId) {
        return metricService.getByDevice(deviceId);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody MetricDto dto,
            Authentication authentication) {
        if (dto.getDeviceId() == null) {
            return ResponseEntity.badRequest().body("Device ID is required");
        }
        if (!deviceRepository.existsById(dto.getDeviceId())) {
            return ResponseEntity.badRequest().body("Device not found");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Device device = deviceRepository.findById(dto.getDeviceId()).get();
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        boolean isOwner = device.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admin or device owner can create metrics");
        }

        try {
            MetricDto created = metricService.create(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody MetricDto dto,
            Authentication authentication) {

        dto.setId(id);
        try {
            MetricDto updated = metricService.update(dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            metricService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
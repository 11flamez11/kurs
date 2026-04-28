package com.example.vkr.controller;

import com.example.vkr.dto.InterfaceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import com.example.vkr.service.InterfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interfaces")
@RequiredArgsConstructor
public class InterfaceController {

    private final InterfaceService interfaceService;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<InterfaceDto> getAll() {
        return interfaceService.getAll();
    }

    @GetMapping("/device/{deviceId}")
    public List<InterfaceDto> getByDevice(@PathVariable Long deviceId) {
        return interfaceService.getByDevice(deviceId);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody InterfaceDto dto,
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
                    .body("Only admin or device owner can create interfaces");
        }

        try {
            InterfaceDto created = interfaceService.create(dto);
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
            @RequestBody InterfaceDto dto,
            Authentication authentication) {
        dto.setId(id);
        try {
            InterfaceDto updated = interfaceService.update(dto);
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
            interfaceService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
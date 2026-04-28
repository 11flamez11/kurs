package com.example.vkr.controller;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import com.example.vkr.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @GetMapping
    public List<DeviceDto> getAll(Authentication authentication) {
        String username = authentication.getName();
        com.example.vkr.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return deviceService.getAll();
        } else {
            return deviceService.getByUser(user.getId());
        }
    }

    @GetMapping("/{id}")
    public DeviceDto getById(@PathVariable Long id) {
        return deviceService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DeviceDto> create(
            @RequestBody DeviceDto dto,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        dto.setUserId(user.getId());
        return ResponseEntity.ok(deviceService.create(dto));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deviceService.delete(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody DeviceDto dto,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))
                && !existingDevice.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admin or owner can update this device");
        }
        try {
            dto.setId(id);
            DeviceDto updated = deviceService.update(dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating device: " + e.getMessage());
        }
    }
}
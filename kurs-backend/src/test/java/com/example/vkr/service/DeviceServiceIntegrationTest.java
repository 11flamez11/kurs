package com.example.vkr.service;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeviceServiceIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        Device device1 = new Device();
        device1.setHostname("Device 1");
        device1.setIpAddress("192.168.1.1");
        device1.setOs("Windows 10");
        device1.setStatus("ONLINE");
        device1.setLastCheck(LocalDateTime.now());
        device1.setUser(testUser);
        deviceRepository.save(device1);

        Device device2 = new Device();
        device2.setHostname("Device 2");
        device2.setIpAddress("192.168.1.2");
        device2.setOs("Linux");
        device2.setStatus("OFFLINE");
        device2.setLastCheck(LocalDateTime.now());
        device2.setUser(testUser);
        deviceRepository.save(device2);
    }

    @Test
    void testGetAll() {
        List<DeviceDto> devices = deviceService.getAll();
        assertEquals(2, devices.size());
    }

    @Test
    void testGetById() {
        List<Device> devices = deviceRepository.findAll();
        Long firstDeviceId = devices.getFirst().getId();

        DeviceDto device = deviceService.getById(firstDeviceId);

        assertNotNull(device);
        assertEquals(firstDeviceId, device.getId());
    }

    @Test
    void testCreate() {
        DeviceDto newDevice = new DeviceDto();
        newDevice.setHostname("Device 3");
        newDevice.setIpAddress("192.168.1.3");
        newDevice.setOs("macOS");
        newDevice.setStatus("ONLINE");
        newDevice.setUserId(testUser.getId());

        DeviceDto created = deviceService.create(newDevice);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Device 3", created.getHostname());
        List<DeviceDto> devices = deviceService.getAll();
        assertEquals(3, devices.size());
    }

    @Test
    void testUpdate() {
        List<Device> devices = deviceRepository.findAll();
        Long firstDeviceId = devices.getFirst().getId();

        DeviceDto updateDto = deviceService.getById(firstDeviceId);
        updateDto.setHostname("Updated Device");
        updateDto.setOs("Windows 11");

        DeviceDto updated = deviceService.update(updateDto);

        assertEquals(firstDeviceId, updated.getId());
        assertEquals("Updated Device", updated.getHostname());
    }

    @Test
    void testDelete() {
        List<Device> devices = deviceRepository.findAll();
        Long firstDeviceId = devices.getFirst().getId();
        int initialSize = devices.size();
        deviceService.delete(firstDeviceId);
        List<DeviceDto> remainingDevices = deviceService.getAll();
        assertEquals(initialSize - 1, remainingDevices.size());
    }
}
package com.example.vkr.service;

import com.example.vkr.dto.DeviceDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.DeviceMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DeviceServiceTest {

    @Test
    public void testGetAll() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        User user = new User();
        user.setId(1L);

        Device device = new Device();
        device.setId(1L);
        device.setHostname("Device 1");
        device.setIpAddress("192.168.1.1");
        device.setOs("Windows 10");
        device.setStatus("ONLINE");
        device.setLastCheck(LocalDateTime.now());
        device.setUser(user);

        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setId(1L);
        deviceDto.setHostname("Device 1");
        deviceDto.setIpAddress("192.168.1.1");
        deviceDto.setOs("Windows 10");
        deviceDto.setStatus("ONLINE");
        deviceDto.setUserId(1L);

        when(deviceRepository.findAll()).thenReturn(List.of(device));
        when(deviceMapper.toDto(device)).thenReturn(deviceDto);

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);

        List<DeviceDto> devices = deviceService.getAll();
        assertEquals(1, devices.size());
        assertEquals(1L, devices.getFirst().getId());
        assertEquals("Device 1", devices.getFirst().getHostname());
    }

    @Test
    public void testGetById() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);

        User user = new User();
        user.setId(1L);

        Device device = new Device();
        device.setId(1L);
        device.setHostname("Test Device");
        device.setIpAddress("10.0.0.1");
        device.setOs("Linux");
        device.setStatus("ONLINE");
        device.setUser(user);

        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setId(1L);
        deviceDto.setHostname("Test Device");
        deviceDto.setIpAddress("10.0.0.1");
        deviceDto.setOs("Linux");
        deviceDto.setStatus("ONLINE");
        deviceDto.setUserId(1L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceMapper.toDto(device)).thenReturn(deviceDto);

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);
        DeviceDto result = deviceService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Device", result.getHostname());
    }

    @Test
    public void testGetById_NotFound() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);

        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);

        assertThrows(RuntimeException.class, () -> deviceService.getById(999L));
    }

    @Test
    public void testCreate() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);

        User user = new User();
        user.setId(1L);

        DeviceDto inputDto = new DeviceDto();
        inputDto.setHostname("New Device");
        inputDto.setIpAddress("192.168.1.100");
        inputDto.setOs("Ubuntu");
        inputDto.setStatus("OFFLINE");
        inputDto.setUserId(1L);

        Device device = new Device();
        device.setId(1L);
        device.setHostname("New Device");
        device.setIpAddress("192.168.1.100");
        device.setOs("Ubuntu");
        device.setStatus("OFFLINE");
        device.setUser(user);

        DeviceDto outputDto = new DeviceDto();
        outputDto.setId(1L);
        outputDto.setHostname("New Device");
        outputDto.setIpAddress("192.168.1.100");
        outputDto.setOs("Ubuntu");
        outputDto.setStatus("OFFLINE");
        outputDto.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(deviceMapper.toEntity(inputDto, user)).thenReturn(device);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(deviceMapper.toDto(device)).thenReturn(outputDto);

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);
        DeviceDto result = deviceService.create(inputDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Device", result.getHostname());
    }

    @Test
    public void testUpdate() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);

        User user = new User();
        user.setId(1L);

        Device existingDevice = new Device();
        existingDevice.setId(1L);
        existingDevice.setHostname("Old Name");
        existingDevice.setIpAddress("192.168.1.1");
        existingDevice.setOs("Windows");
        existingDevice.setStatus("ONLINE");
        existingDevice.setUser(user);

        DeviceDto updateDto = new DeviceDto();
        updateDto.setId(1L);
        updateDto.setHostname("Updated Name");
        updateDto.setOs("Linux");

        Device updatedDevice = new Device();
        updatedDevice.setId(1L);
        updatedDevice.setHostname("Updated Name");
        updatedDevice.setIpAddress("192.168.1.1");
        updatedDevice.setOs("Linux");
        updatedDevice.setStatus("ONLINE");
        updatedDevice.setUser(user);

        DeviceDto outputDto = new DeviceDto();
        outputDto.setId(1L);
        outputDto.setHostname("Updated Name");
        outputDto.setIpAddress("192.168.1.1");
        outputDto.setOs("Linux");
        outputDto.setStatus("ONLINE");
        outputDto.setUserId(1L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(updatedDevice);
        when(deviceMapper.toDto(updatedDevice)).thenReturn(outputDto);

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);
        DeviceDto result = deviceService.update(updateDto);

        assertNotNull(result);
        assertEquals("Updated Name", result.getHostname());
        assertEquals("Linux", result.getOs());
        assertEquals("192.168.1.1", result.getIpAddress()); // Не изменился
    }

    @Test
    public void testDelete() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);

        doNothing().when(deviceRepository).deleteById(1L);

        DeviceService deviceService = new DeviceService(deviceRepository, userRepository, deviceMapper);

        assertDoesNotThrow(() -> deviceService.delete(1L));
        verify(deviceRepository, times(1)).deleteById(1L);
    }
}
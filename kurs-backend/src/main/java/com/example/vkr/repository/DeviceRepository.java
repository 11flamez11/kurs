package com.example.vkr.repository;

import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUser(User user);

    Optional<Device> findByUserAndHostnameAndIpAddress(User user, String hostname, String ipAddress);
}

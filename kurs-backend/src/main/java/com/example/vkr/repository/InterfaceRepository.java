package com.example.vkr.repository;

import com.example.vkr.entity.Interface;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterfaceRepository extends JpaRepository<Interface, Long> {

    List<Interface> findByDevice(Device device);

    List<Interface> findByDeviceUser(User user);

    Optional<Interface> findByDeviceAndName(Device device, String name);
}

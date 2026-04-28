package com.example.vkr.repository;

import com.example.vkr.entity.Interface;
import com.example.vkr.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterfaceRepository extends JpaRepository<Interface, Long> {

    List<Interface> findByDevice(Device device);
}

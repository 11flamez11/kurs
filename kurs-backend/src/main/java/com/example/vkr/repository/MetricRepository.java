package com.example.vkr.repository;

import com.example.vkr.entity.Metric;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByDevice(Device device);

    List<Metric> findByDeviceUser(User user);
}
package com.example.vkr.repository;

import com.example.vkr.entity.Event;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByDevice(Device device);

    List<Event> findByDeviceUser(User user);

    Event findTopByDeviceAndSeverityAndDescriptionOrderByTimestampDesc(Device device, String severity, String description);

    Event findTopByDeviceAndSeverityAndDescriptionStartingWithOrderByTimestampDesc(
            Device device,
            String severity,
            String descriptionPrefix
    );
}
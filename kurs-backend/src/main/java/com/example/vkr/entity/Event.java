package com.example.vkr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private LocalDateTime timestamp;

    @Column(length = 20)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean resolved;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
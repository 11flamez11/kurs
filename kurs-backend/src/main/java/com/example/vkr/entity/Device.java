package com.example.vkr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String hostname;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 50)
    private String os;

    @Column(length = 20)
    private String status;

    private LocalDateTime lastCheck;
}
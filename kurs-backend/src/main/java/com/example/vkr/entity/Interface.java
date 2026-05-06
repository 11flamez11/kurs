package com.example.vkr.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interfaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(length = 50)
    private String name;

    @Column(length = 17)
    private String macAddress;

    private int speed;

    @Column(length = 10)
    private String status;
}
package com.rc.tracking.model.entity;

import com.rc.tracking.model.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packages")
public class PackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String sender;
    private String recipient;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "is_holliday")
    private Boolean isHolliday;

    @Column(name = "fun_fact")
    private String funFact;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @OneToMany(mappedBy = "packageEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TrackingEvent> events = new ArrayList<>();
}
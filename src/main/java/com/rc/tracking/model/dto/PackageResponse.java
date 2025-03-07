package com.rc.tracking.model.dto;

import com.rc.tracking.model.enums.StatusEnum;

import java.time.LocalDateTime;

public record PackageResponse(
        String id,
        String description,
        String sender,
        String recipient,
        StatusEnum status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deliveredAt
) {}

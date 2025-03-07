package com.rc.tracking.model.dto;

import com.rc.tracking.model.enums.StatusEnum;

import java.time.LocalDateTime;
import java.util.List;

public record PackageDetailResponse(
        String id,
        String description,
        String sender,
        String recipient,
        StatusEnum status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<EventDTO> events
) {}

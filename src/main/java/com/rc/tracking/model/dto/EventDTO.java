package com.rc.tracking.model.dto;

import java.time.LocalDateTime;

public record EventDTO(
        String packageId,
        String location,
        String description,
        LocalDateTime dateTime
) {}

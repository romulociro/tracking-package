package com.rc.tracking.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TrackingEventRequest(
        @NotBlank String packageId,
        @NotBlank String location,
        @NotBlank String description,
        @NotNull LocalDateTime date
) {}

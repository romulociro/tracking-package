package com.rc.tracking.model.dto;

import java.time.LocalDate;

public record PackageRequest(
        String description,
        String sender,
        String recipient,
        LocalDate estimatedDeliveryDate
) {}
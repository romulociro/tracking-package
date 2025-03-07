package com.rc.tracking.model.dto;

import java.time.LocalDate;

public record Holiday(
        LocalDate date,
        String localName,
        String name
) {}

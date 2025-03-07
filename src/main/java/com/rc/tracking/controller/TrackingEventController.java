package com.rc.tracking.controller;

import com.rc.tracking.model.dto.TrackingEventRequest;
import com.rc.tracking.service.TrackingEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking-events")
@RequiredArgsConstructor
public class TrackingEventController {

    private final TrackingEventService trackingEventService;

    /**
     * Endpoint para envio de eventos de rastreamento.
     * Retorna 202 Accepted, indicando que o evento foi aceito para processamento assíncrono.
     */
    @PostMapping
    public ResponseEntity<Void> createTrackingEvent(@Valid @RequestBody TrackingEventRequest request) {
        trackingEventService.processTrackingEvent(request);
        return ResponseEntity.accepted().build();
    }
}

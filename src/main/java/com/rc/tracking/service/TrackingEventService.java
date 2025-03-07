package com.rc.tracking.service;

import com.rc.tracking.exception.ResourceNotFoundException;
import com.rc.tracking.model.dto.TrackingEventRequest;
import com.rc.tracking.model.entity.PackageEntity;
import com.rc.tracking.model.entity.TrackingEvent;
import com.rc.tracking.repository.PackageRepository;
import com.rc.tracking.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrackingEventService {

    private final TrackingEventRepository trackingEventRepository;
    private final PackageRepository packageRepository;

    private static final Logger logger = LoggerFactory.getLogger(TrackingEventService.class);

    @Async
    @Transactional
    @Retryable(
            value = { DeadlockLoserDataAccessException.class,
                    org.springframework.dao.DeadlockLoserDataAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void processTrackingEvent(TrackingEventRequest request) {
        Long packageId;
        try {
            String idStr = request.packageId().replace("packageEntity-", "");
            packageId = Long.parseLong(idStr);
        } catch (Exception e) {
            logger.error("Invalid packageId format: {}", request.packageId());
            throw new IllegalArgumentException("Invalid packageId format");
        }

        PackageEntity packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        if (request.date().isBefore(packageEntity.getCreatedAt())) {
            logger.error("Event date {} is before package creation date {}", request.date(), packageEntity.getCreatedAt());
            throw new IllegalArgumentException("Event date cannot be before package creation date");
        }

        TrackingEvent trackingEvent = TrackingEvent.builder()
                .location(request.location())
                .description(request.description())
                .dateTime(request.date())
                .packageEntity(packageEntity)
                .build();

        trackingEventRepository.save(trackingEvent);

        packageEntity.setUpdatedAt(LocalDateTime.now());
        packageRepository.save(packageEntity);

        logger.info("Tracking event processed for package id: {}", packageId);
    }

    @Recover
    public void recover(Exception ex, TrackingEventRequest request) {
        logger.error("Failed to process tracking event for package {} after retries: {}", request.packageId(), ex.getMessage());
        throw new RuntimeException("Unable to process tracking event after multiple retries", ex);
    }
}

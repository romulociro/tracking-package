package com.rc.tracking.service;

import com.rc.tracking.model.dto.TrackingEventRequest;
import com.rc.tracking.model.entity.PackageEntity;
import com.rc.tracking.model.entity.TrackingEvent;
import com.rc.tracking.model.enums.StatusEnum;
import com.rc.tracking.repository.PackageRepository;
import com.rc.tracking.repository.TrackingEventRepository;
import com.rc.tracking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrackingEventServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private PackageRepository packageRepository;

    @InjectMocks
    private TrackingEventService trackingEventService;

    private PackageEntity packageEntity;
    private TrackingEventRequest validRequest;

    @BeforeEach
    public void setup() {
        packageEntity = PackageEntity.builder()
                .id(1L)
                .description("Test Package")
                .sender("Sender A")
                .recipient("Recipient B")
                .status(StatusEnum.valueOf("CREATED"))
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        validRequest = new TrackingEventRequest(
                "packageEntity-1",
                "Warehouse",
                "Package reached warehouse",
                LocalDateTime.now()
        );
    }

    @Test
    public void testProcessTrackingEvent_Success() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(trackingEventRepository.save(any(TrackingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(PackageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trackingEventService.processTrackingEvent(validRequest);

        verify(trackingEventRepository, times(1)).save(any(TrackingEvent.class));
        verify(packageRepository, times(1)).save(any(PackageEntity.class));
    }

    @Test
    public void testProcessTrackingEvent_InvalidPackageIdFormat() {
        TrackingEventRequest requestInvalidId = new TrackingEventRequest(
                "invalid-id",
                "Warehouse",
                "Package reached warehouse",
                LocalDateTime.now()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                trackingEventService.processTrackingEvent(requestInvalidId));
        assertEquals("Invalid packageId format", exception.getMessage());
        verify(packageRepository, never()).findById(anyLong());
        verify(trackingEventRepository, never()).save(any(TrackingEvent.class));
    }

    @Test
    public void testProcessTrackingEvent_PackageNotFound() {
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                trackingEventService.processTrackingEvent(validRequest));
        assertTrue(exception.getMessage().contains("Package not found with id: 1"));
        verify(trackingEventRepository, never()).save(any(TrackingEvent.class));
    }

    @Test
    public void testProcessTrackingEvent_EventDateBeforePackageCreation() {
        TrackingEventRequest requestWithPastDate = new TrackingEventRequest(
                "packageEntity-1",
                "Warehouse",
                "Package reached warehouse",
                packageEntity.getCreatedAt().minusMinutes(10)
        );
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                trackingEventService.processTrackingEvent(requestWithPastDate));
        assertEquals("Event date cannot be before package creation date", exception.getMessage());
        verify(trackingEventRepository, never()).save(any(TrackingEvent.class));
    }
}

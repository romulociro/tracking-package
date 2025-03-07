package com.rc.tracking.service;

import com.rc.tracking.exception.InvalidStatusTransitionException;
import com.rc.tracking.exception.PackageCannotBeCancelledException;
import com.rc.tracking.mapper.PackageMapper;
import com.rc.tracking.model.dto.EventDTO;
import com.rc.tracking.model.dto.PackageDetailResponse;
import com.rc.tracking.model.dto.PackageRequest;
import com.rc.tracking.model.dto.PackageResponse;
import com.rc.tracking.model.entity.PackageEntity;
import com.rc.tracking.model.entity.TrackingEvent;
import com.rc.tracking.model.enums.StatusEnum;
import com.rc.tracking.repository.PackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.rc.tracking.model.enums.StatusEnum.DELIVERED;
import static com.rc.tracking.model.enums.StatusEnum.IN_TRANSIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PackageServiceTest {

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private PackageMapper packageMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PackageService packageService;

    private PackageRequest packageRequest;
    private PackageEntity packageEntity;

    @BeforeEach
    public void setup() {
        packageRequest = new PackageRequest(
                "Test Package",
                "Sender A",
                "Recipient B",
                LocalDate.now().plusDays(1)
        );
        packageEntity = PackageEntity.builder()
                .id(1L)
                .description(packageRequest.description())
                .sender(packageRequest.sender())
                .recipient(packageRequest.recipient())
                .estimatedDeliveryDate(packageRequest.estimatedDeliveryDate())
                .isHolliday(false)
                .funFact("Test Dog Fact")
                .status(StatusEnum.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testCreatePackage() {
        when(packageRepository.save(any(PackageEntity.class))).thenReturn(packageEntity);
        PackageResponse expectedResponse = new PackageResponse("packageEntity-1",
                packageRequest.description(),
                packageRequest.sender(),
                packageRequest.recipient(),
                StatusEnum.CREATED,
                packageEntity.getCreatedAt(),
                packageEntity.getDeliveredAt(),
                packageEntity.getUpdatedAt());

        when(packageMapper.packageEntityToPackageResponse(packageEntity)).thenReturn(expectedResponse);

        PackageResponse response = packageService.createPackage(packageRequest);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testUpdateStatus_ValidTransition_CreatedToInTransit() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        PackageEntity updatedEntity = PackageEntity.builder()
                .id(1L)
                .description(packageEntity.getDescription())
                .sender(packageEntity.getSender())
                .recipient(packageEntity.getRecipient())
                .estimatedDeliveryDate(packageEntity.getEstimatedDeliveryDate())
                .isHolliday(packageEntity.getIsHolliday())
                .funFact(packageEntity.getFunFact())
                .status(IN_TRANSIT)
                .createdAt(packageEntity.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(packageRepository.save(any(PackageEntity.class))).thenReturn(updatedEntity);
        PackageResponse expectedResponse = new PackageResponse("packageEntity-1",
                updatedEntity.getDescription(),
                updatedEntity.getSender(),
                updatedEntity.getRecipient(),
                IN_TRANSIT,
                updatedEntity.getCreatedAt(),
                updatedEntity.getDeliveredAt(),
                updatedEntity.getUpdatedAt());
        when(packageMapper.packageEntityToPackageResponse(updatedEntity)).thenReturn(expectedResponse);

        PackageResponse response = packageService.updateStatus(1L, IN_TRANSIT);

        assertEquals(IN_TRANSIT, response.status());
    }

    @Test
    public void testUpdateStatus_InvalidTransition() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));

        assertThrows(InvalidStatusTransitionException.class, () ->
                packageService.updateStatus(1L, DELIVERED));
    }

    @Test
    public void testCancelPackage_Success() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        PackageEntity cancelledEntity = PackageEntity.builder()
                .id(1L)
                .description(packageEntity.getDescription())
                .sender(packageEntity.getSender())
                .recipient(packageEntity.getRecipient())
                .estimatedDeliveryDate(packageEntity.getEstimatedDeliveryDate())
                .isHolliday(packageEntity.getIsHolliday())
                .funFact(packageEntity.getFunFact())
                .status(StatusEnum.CANCELLED)
                .createdAt(packageEntity.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(packageRepository.save(any(PackageEntity.class))).thenReturn(cancelledEntity);
        PackageResponse expectedResponse = new PackageResponse("packageEntity-1",
                cancelledEntity.getDescription(),
                cancelledEntity.getSender(),
                cancelledEntity.getRecipient(),
                StatusEnum.CANCELLED,
                cancelledEntity.getDeliveredAt(),
                cancelledEntity.getCreatedAt(),
                cancelledEntity.getUpdatedAt());
        when(packageMapper.packageEntityToPackageResponse(cancelledEntity)).thenReturn(expectedResponse);

        PackageResponse response = packageService.cancelPackage(1L);

        assertEquals(StatusEnum.CANCELLED, response.status());
    }

    @Test
    public void testCancelPackage_Failure() {
        packageEntity.setStatus(IN_TRANSIT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));

        assertThrows(PackageCannotBeCancelledException.class, () ->
                packageService.cancelPackage(1L));
    }

    @Test
    public void testGetPackageDetails_WithAndWithoutEvents() {
        TrackingEvent trackingEvent = TrackingEvent.builder()
                .id(10L)
                .location("Warehouse")
                .description("Package reached warehouse")
                .dateTime(LocalDateTime.of(2025, 10, 10, 12, 0))
                .packageEntity(packageEntity)
                .build();

        packageEntity.setEvents(Arrays.asList(trackingEvent));

        List<EventDTO> expectedEvents = Arrays.asList(
                new EventDTO("packageEntity-" + packageEntity.getId(),
                        trackingEvent.getLocation(),
                        trackingEvent.getDescription(),
                        trackingEvent.getDateTime())
        );

        PackageDetailResponse expectedDetail = new PackageDetailResponse(
                "packageEntity-" + packageEntity.getId(),
                packageEntity.getDescription(),
                packageEntity.getSender(),
                packageEntity.getRecipient(),
                packageEntity.getStatus(),
                packageEntity.getCreatedAt(),
                packageEntity.getUpdatedAt(),
                expectedEvents
        );

        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(packageMapper.packageEntityToPackageDetailResponse(argThat(
                entity -> entity != null && entity.getEvents() != null && !entity.getEvents().isEmpty()
        ))).thenReturn(expectedDetail);

        PackageDetailResponse responseWithEvents = packageService.getPackageDetails(1L, true);
        assertNotNull(responseWithEvents);
        assertEquals(expectedDetail, responseWithEvents);

        PackageEntity entityWithoutEvents = PackageEntity.builder()
                .id(packageEntity.getId())
                .description(packageEntity.getDescription())
                .sender(packageEntity.getSender())
                .recipient(packageEntity.getRecipient())
                .status(packageEntity.getStatus())
                .createdAt(packageEntity.getCreatedAt())
                .updatedAt(packageEntity.getUpdatedAt())
                .events(null)
                .build();

        PackageDetailResponse expectedNoEvents = new PackageDetailResponse(
                "packageEntity-" + entityWithoutEvents.getId(),
                entityWithoutEvents.getDescription(),
                entityWithoutEvents.getSender(),
                entityWithoutEvents.getRecipient(),
                entityWithoutEvents.getStatus(),
                entityWithoutEvents.getCreatedAt(),
                entityWithoutEvents.getUpdatedAt(),
                null
        );

        when(packageRepository.findById(1L)).thenReturn(Optional.of(entityWithoutEvents));
        when(packageMapper.packageEntityToPackageDetailResponse(argThat(
                entity -> entity != null && entity.getEvents() == null
        ))).thenReturn(expectedNoEvents);

        PackageDetailResponse responseWithoutEvents = packageService.getPackageDetails(1L, false);
        assertNotNull(responseWithoutEvents);
        assertNull(responseWithoutEvents.events());
        assertEquals(expectedNoEvents, responseWithoutEvents);
    }

    @Test
    public void testListPackages() {
        PackageEntity packageEntity2 = PackageEntity.builder()
                .id(2L)
                .description("Another Package")
                .sender("Sender A")
                .recipient("Recipient C")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .isHolliday(false)
                .funFact("Another fact")
                .status(StatusEnum.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<PackageEntity> entities = Arrays.asList(packageEntity, packageEntity2);
        when(packageRepository.findAll()).thenReturn(entities);

        PackageResponse response1 = new PackageResponse("packageEntity-1", packageEntity.getDescription(),
                packageEntity.getSender(), packageEntity.getRecipient(), StatusEnum.CREATED,
                packageEntity.getCreatedAt(), packageEntity.getUpdatedAt(), packageEntity.getDeliveredAt());
        PackageResponse response2 = new PackageResponse("packageEntity-2", packageEntity2.getDescription(),
                packageEntity2.getSender(), packageEntity2.getRecipient(), StatusEnum.CREATED,
                packageEntity2.getCreatedAt(), packageEntity2.getUpdatedAt(), packageEntity.getDeliveredAt());

        when(packageMapper.packageEntityToPackageResponse(packageEntity)).thenReturn(response1);
        when(packageMapper.packageEntityToPackageResponse(packageEntity2)).thenReturn(response2);

        List<PackageResponse> responses = packageService.listPackages(null, null);

        assertEquals(2, responses.size());
        assertTrue(responses.contains(response1));
        assertTrue(responses.contains(response2));
    }

    @Test
    public void testUpdateStatus_Delivered_FillsDeliveredAt() {
        packageEntity.setStatus(IN_TRANSIT);
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusHours(1);
        packageEntity.setCreatedAt(originalCreatedAt);
        packageEntity.setUpdatedAt(originalCreatedAt);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));

        PackageEntity updatedEntity = PackageEntity.builder()
                .id(packageEntity.getId())
                .description(packageEntity.getDescription())
                .sender(packageEntity.getSender())
                .recipient(packageEntity.getRecipient())
                .estimatedDeliveryDate(packageEntity.getEstimatedDeliveryDate())
                .isHolliday(packageEntity.getIsHolliday())
                .funFact(packageEntity.getFunFact())
                .status(DELIVERED)
                .createdAt(packageEntity.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .build();

        when(packageRepository.save(any(PackageEntity.class))).thenReturn(updatedEntity);

        PackageResponse expectedResponse = new PackageResponse(
                "packageEntity-" + updatedEntity.getId(),
                updatedEntity.getDescription(),
                updatedEntity.getSender(),
                updatedEntity.getRecipient(),
                DELIVERED,
                updatedEntity.getCreatedAt(),
                updatedEntity.getUpdatedAt(),
                updatedEntity.getDeliveredAt()
        );
        when(packageMapper.packageEntityToPackageResponse(updatedEntity)).thenReturn(expectedResponse);

        PackageResponse response = packageService.updateStatus(1L, DELIVERED);

        assertNotNull(response.deliveredAt(), "deliveredAt should be filled when status is DELIVERED");
        assertTrue(response.deliveredAt().isAfter(originalCreatedAt),
                "deliveredAt should be after the creation date");
        assertEquals(DELIVERED, response.status(), "Status should be DELIVERED");
    }
}
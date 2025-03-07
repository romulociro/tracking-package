package com.rc.tracking.service;

import com.rc.tracking.exception.InvalidStatusTransitionException;
import com.rc.tracking.exception.PackageCannotBeCancelledException;
import com.rc.tracking.exception.ResourceNotFoundException;
import com.rc.tracking.mapper.PackageMapper;
import com.rc.tracking.model.dto.*;
import com.rc.tracking.model.entity.PackageEntity;
import com.rc.tracking.model.enums.StatusEnum;
import com.rc.tracking.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.rc.tracking.model.enums.StatusEnum.*;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;
    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(PackageService.class);

    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        boolean isHoliday = checkHoliday(request.estimatedDeliveryDate());
        String funFact = fetchDogFunFact();

        PackageEntity packageEntity = PackageEntity.builder()
                .description(request.description())
                .sender(request.sender())
                .recipient(request.recipient())
                .estimatedDeliveryDate(request.estimatedDeliveryDate())
                .isHolliday(isHoliday)
                .funFact(funFact)
                .status(CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PackageEntity savedEntity = packageRepository.save(packageEntity);
        return packageMapper.packageEntityToPackageResponse(savedEntity);
    }
    @Retryable(
            value = DeadlockLoserDataAccessException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public PackageResponse updateStatus(Long packageId, StatusEnum newStatus) {
        PackageEntity packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        StatusEnum currentStatus = packageEntity.getStatus();

        if (currentStatus == StatusEnum.CREATED && newStatus == StatusEnum.IN_TRANSIT) {
            packageEntity.setStatus(newStatus);
        } else if (currentStatus == StatusEnum.IN_TRANSIT && newStatus == StatusEnum.DELIVERED) {
            packageEntity.setStatus(newStatus);
            packageEntity.setDeliveredAt(LocalDateTime.now());
        } else {
            throw new InvalidStatusTransitionException("Invalid status transition from "
                    + currentStatus + " to " + newStatus);
        }
        packageEntity.setUpdatedAt(LocalDateTime.now());
        PackageEntity updatedEntity = packageRepository.save(packageEntity);
        return packageMapper.packageEntityToPackageResponse(updatedEntity);
    }

    @Transactional
    public PackageResponse cancelPackage(Long packageId) {
        PackageEntity packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        StatusEnum currentStatus = packageEntity.getStatus();
        if (currentStatus != CREATED) {
            throw new PackageCannotBeCancelledException("Package cannot be cancelled. Current status: " + currentStatus);
        }
        packageEntity.setStatus(CANCELLED);
        packageEntity.setUpdatedAt(LocalDateTime.now());
        PackageEntity updatedEntity = packageRepository.save(packageEntity);
        return packageMapper.packageEntityToPackageResponse(updatedEntity);
    }

    public PackageDetailResponse getPackageDetails(Long packageId, boolean includeEvents) {
        PackageEntity packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));
        if (!includeEvents) {
            packageEntity.setEvents(null);
        }
        return packageMapper.packageEntityToPackageDetailResponse(packageEntity);
    }

    public List<PackageResponse> listPackages(String sender, String recipient) {
        List<PackageEntity> packageEntities;

        if (sender != null && !sender.isEmpty() && recipient != null && !recipient.isEmpty()) {
            packageEntities = packageRepository.findBySenderContainingIgnoreCaseAndRecipientContainingIgnoreCase(sender, recipient);
        } else if (sender != null && !sender.isEmpty()) {
            packageEntities = packageRepository.findBySenderContainingIgnoreCase(sender);
        } else if (recipient != null && !recipient.isEmpty()) {
            packageEntities = packageRepository.findByRecipientContainingIgnoreCase(recipient);
        } else {
            packageEntities = packageRepository.findAll();
        }

        return packageEntities.stream()
                .map(packageMapper::packageEntityToPackageResponse)
                .collect(Collectors.toList());
    }

    private boolean checkHoliday(LocalDate estimatedDate) {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + estimatedDate.getYear() + "/BR";
        try {
            ResponseEntity<Holiday[]> response = restTemplate.getForEntity(url, Holiday[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.stream(response.getBody())
                        .anyMatch(holiday -> holiday.date().equals(estimatedDate));
            }
        } catch (Exception ex) {
            logger.error("Error checking holiday for date {}: {}", estimatedDate, ex.getMessage());
        }
        return false;
    }

    private String fetchDogFunFact() {
        String url = "https://dogapi.dog/api/v1/facts";
        try {
            ResponseEntity<DogFactResponse> response = restTemplate.getForEntity(url, DogFactResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().fact();
            }
        } catch (Exception ex) {
            logger.error("Error fetching dog fun fact: {}", ex.getMessage());
        }
        return "Fun fact not available";
    }

    @Recover
    public PackageResponse recover(DeadlockLoserDataAccessException ex, Long packageId, StatusEnum newStatus) {
        logger.error("Failed to update status for package {} after retries: {}", packageId, ex.getMessage());
        throw new RuntimeException("Unable to update package status after multiple retries", ex);
    }
}
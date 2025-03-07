package com.rc.tracking.repository;

import com.rc.tracking.model.entity.PackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<PackageEntity, Long> {

    List<PackageEntity> findBySenderContainingIgnoreCase(String sender);

    List<PackageEntity> findByRecipientContainingIgnoreCase(String recipient);

    List<PackageEntity> findBySenderContainingIgnoreCaseAndRecipientContainingIgnoreCase(String sender, String recipient);

    int deleteByDeliveredAtBefore(LocalDateTime cutoff);

}

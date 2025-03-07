package com.rc.tracking.scheduled;

import com.rc.tracking.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DatabasePurgeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePurgeScheduler.class);

    private final PackageRepository packageRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeOldPackages() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
        int deletedCount = packageRepository.deleteByDeliveredAtBefore(cutoffDate);
        logger.info("Expurgo concluído: {} pacotes removidos (entregues antes de {})", deletedCount, cutoffDate);
    }
}

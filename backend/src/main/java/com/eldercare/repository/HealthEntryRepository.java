package com.eldercare.repository;

import com.eldercare.model.HealthEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthEntryRepository extends JpaRepository<HealthEntry, Long> {

    List<HealthEntry> findByElderlyIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long elderlyId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    List<HealthEntry> findByElderlyIdOrderByRecordedAtDesc(Long elderlyId, Pageable pageable);
}


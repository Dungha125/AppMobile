package com.eldercare.repository;

import com.eldercare.model.MedicationHistory;
import com.eldercare.model.enums.MedicationHistoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationHistoryRepository extends JpaRepository<MedicationHistory, Long> {

    List<MedicationHistory> findByMedicationScheduleIdOrderByScheduledTimeDesc(Long scheduleId, org.springframework.data.domain.Pageable pageable);

    List<MedicationHistory> findByMedicationSchedule_Medication_Prescription_Elderly_IdAndScheduledTimeBetween(
            Long elderlyId, LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(
            MedicationHistoryStatus status, LocalDateTime start, LocalDateTime end);
}

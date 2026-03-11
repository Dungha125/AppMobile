package com.eldercare.service;

import com.eldercare.model.MedicationHistory;
import com.eldercare.model.MedicationSchedule;
import com.eldercare.model.enums.MedicationHistoryStatus;
import com.eldercare.repository.MedicationHistoryRepository;
import com.eldercare.repository.MedicationScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationHistoryService {

    private final MedicationHistoryRepository historyRepository;
    private final MedicationScheduleRepository scheduleRepository;

    public MedicationHistory confirmTaken(Long scheduleId, LocalDateTime scheduledTime) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch uống thuốc"));

        MedicationHistory history = MedicationHistory.builder()
                .medicationSchedule(schedule)
                .scheduledTime(scheduledTime)
                .takenAt(LocalDateTime.now())
                .status(MedicationHistoryStatus.TAKEN)
                .build();
        return historyRepository.save(history);
    }

    public List<MedicationHistory> getHistoryByElderly(Long elderlyId, LocalDateTime start, LocalDateTime end) {
        return historyRepository.findByMedicationSchedule_Medication_Prescription_Elderly_IdAndScheduledTimeBetween(
                elderlyId, start, end);
    }

    public List<MedicationHistory> getHistoryBySchedule(Long scheduleId, int limit) {
        return historyRepository.findByMedicationScheduleIdOrderByScheduledTimeDesc(
                scheduleId, PageRequest.of(0, limit));
    }
}

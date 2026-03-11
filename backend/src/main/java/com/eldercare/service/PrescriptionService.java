package com.eldercare.service;

import com.eldercare.model.*;
import com.eldercare.repository.MedicationRepository;
import com.eldercare.repository.MedicationScheduleRepository;
import com.eldercare.repository.PrescriptionRepository;
import com.eldercare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public Prescription create(Prescription prescription, Long createdById) {
        User creator = userRepository.findById(createdById).orElse(null);
        prescription.setCreatedBy(creator);
        return prescriptionRepository.save(prescription);
    }

    public Prescription update(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    public void delete(Long id) {
        prescriptionRepository.deleteById(id);
    }

    public Prescription findById(Long id) {
        // Optimize: Lấy prescription với medications và schedules một lần
        return prescriptionRepository.findByIdWithMedications(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc"));
    }

    public List<Prescription> findByElderlyId(Long elderlyId) {
        // Optimize: JOIN FETCH medications và schedules để tránh N+1 queries
        return prescriptionRepository.findByElderlyIdWithMedications(elderlyId);
    }

    @Transactional
    public Medication addMedication(Long prescriptionId, Medication medication) {
        Prescription prescription = findById(prescriptionId);
        medication.setPrescription(prescription);
        return medicationRepository.save(medication);
    }

    @Transactional
    public MedicationSchedule addSchedule(Long medicationId, LocalTime timeOfDay, Integer reminderMinutes) {
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));
        MedicationSchedule schedule = MedicationSchedule.builder()
                .medication(medication)
                .timeOfDay(timeOfDay)
                .dayOfWeek("ALL")
                .reminderMinutesBefore(reminderMinutes != null ? reminderMinutes : 15)
                .isActive(true)
                .build();
        return scheduleRepository.save(schedule);
    }
}

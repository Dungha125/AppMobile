package com.eldercare.dto;

import com.eldercare.model.Medication;
import com.eldercare.model.Prescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO cho Prescription để tránh lazy loading issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDto {
    private Long id;
    private String title;
    private String doctorName;
    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Elderly info
    private Long elderlyId;
    private String elderlyName;
    
    // Created by
    private Long createdById;
    private String createdByName;
    
    // Medications
    private List<MedicationDto> medications;

    public static PrescriptionDto fromPrescription(Prescription prescription) {
        if (prescription == null) return null;
        
        return PrescriptionDto.builder()
                .id(prescription.getId())
                .title(prescription.getTitle())
                .doctorName(prescription.getDoctorName())
                .notes(prescription.getNotes())
                .startDate(prescription.getStartDate())
                .endDate(prescription.getEndDate())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .elderlyId(prescription.getElderly() != null ? prescription.getElderly().getId() : null)
                .elderlyName(prescription.getElderly() != null ? prescription.getElderly().getFullName() : null)
                .createdById(prescription.getCreatedBy() != null ? prescription.getCreatedBy().getId() : null)
                .createdByName(prescription.getCreatedBy() != null ? prescription.getCreatedBy().getFullName() : null)
                .medications(prescription.getMedications() != null ? 
                        prescription.getMedications().stream()
                                .map(MedicationDto::fromMedication)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationDto {
        private Long id;
        private String name;
        private String dosage;
        private String unit;
        private Integer quantity;
        private String instructions;
        private Integer scheduleCount;

        public static MedicationDto fromMedication(Medication medication) {
            if (medication == null) return null;
            
            return MedicationDto.builder()
                    .id(medication.getId())
                    .name(medication.getName())
                    .dosage(medication.getDosage())
                    .unit(medication.getUnit())
                    .quantity(medication.getQuantity())
                    .instructions(medication.getInstructions())
                    .scheduleCount(medication.getSchedules() != null ? 
                            medication.getSchedules().size() : 0)
                    .build();
        }
    }
}

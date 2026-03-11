package com.eldercare.controller;

import com.eldercare.dto.ApiResponse;
import com.eldercare.dto.PrescriptionDto;
import com.eldercare.model.Medication;
import com.eldercare.model.MedicationSchedule;
import com.eldercare.model.Prescription;
import com.eldercare.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/elderly/{elderlyId}")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getByElderly(@PathVariable Long elderlyId) {
        List<Prescription> list = prescriptionService.findByElderlyId(elderlyId);
        List<PrescriptionDto> dtos = list.stream()
                .map(PrescriptionDto::fromPrescription)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionDto>> getById(@PathVariable Long id) {
        Prescription p = prescriptionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(PrescriptionDto.fromPrescription(p)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Prescription>> create(@RequestBody Prescription prescription,
                                                            @RequestParam Long createdBy) {
        Prescription saved = prescriptionService.create(prescription, createdBy);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Prescription>> update(@PathVariable Long id, @RequestBody Prescription body) {
        Prescription existing = prescriptionService.findById(id);
        body.setId(existing.getId());
        body.setElderly(existing.getElderly());
        Prescription saved = prescriptionService.update(body);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        prescriptionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa", "OK"));
    }

    @PostMapping("/{prescriptionId}/medications")
    public ResponseEntity<ApiResponse<Medication>> addMedication(@PathVariable Long prescriptionId,
                                                                 @RequestBody Medication medication) {
        Medication saved = prescriptionService.addMedication(prescriptionId, medication);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PostMapping("/medications/{medicationId}/schedules")
    public ResponseEntity<ApiResponse<MedicationSchedule>> addSchedule(@PathVariable Long medicationId,
                                                                       @RequestBody Map<String, Object> body) {
        String timeStr = (String) body.get("timeOfDay"); // "08:00"
        Integer reminder = body.get("reminderMinutesBefore") != null
                ? (Integer) body.get("reminderMinutesBefore") : 15;
        LocalTime time = LocalTime.parse(timeStr != null ? timeStr : "08:00");
        MedicationSchedule schedule = prescriptionService.addSchedule(medicationId, time, reminder);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }
}

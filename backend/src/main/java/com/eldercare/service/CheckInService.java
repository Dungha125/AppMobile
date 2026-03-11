package com.eldercare.service;

import com.eldercare.model.CheckIn;
import com.eldercare.model.User;
import com.eldercare.model.enums.CheckInType;
import com.eldercare.repository.CheckInRepository;
import com.eldercare.repository.ElderlyProfileRepository;
import com.eldercare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final ElderlyProfileRepository elderlyProfileRepository;

    public CheckIn createCheckIn(Long elderlyId, CheckInType type, String notes, BigDecimal lat, BigDecimal lng) {
        User elderly = userRepository.findById(elderlyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người cao tuổi"));

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        // Kiểm tra đã điểm danh hôm nay chưa
        var existingCheckIn = checkInRepository.findFirstByElderlyIdAndCheckedAtBetweenOrderByCheckedAtDesc(
                elderlyId, startOfDay, endOfDay);
        
        if (existingCheckIn.isPresent()) {
            CheckIn existing = existingCheckIn.get();
            
            // Nếu có ACTIVE rồi → Không cho điểm danh nữa
            if (existing.getCheckInType() == CheckInType.ACTIVE) {
                throw new RuntimeException("Bạn đã điểm danh hôm nay rồi. Mỗi ngày chỉ điểm danh 1 lần.");
            }
            
            // Nếu có PASSIVE rồi, nhưng đang tạo ACTIVE → Cho phép (upgrade to ACTIVE)
            if (existing.getCheckInType() == CheckInType.PASSIVE && type == CheckInType.ACTIVE) {
                // Xóa PASSIVE cũ, tạo ACTIVE mới (hoặc update existing)
                existing.setCheckInType(CheckInType.ACTIVE);
                existing.setNotes(notes);
                if (lat != null) existing.setLatitude(lat);
                if (lng != null) existing.setLongitude(lng);
                checkInRepository.save(existing);
                
                updateElderlyProfile(elderlyId, lat, lng);
                return existing;
            }
            
            // Nếu có PASSIVE rồi, đang tạo PASSIVE nữa → Skip
            if (existing.getCheckInType() == CheckInType.PASSIVE && type == CheckInType.PASSIVE) {
                throw new RuntimeException("Đã có điểm danh tự động hôm nay rồi.");
            }
        }

        // Tạo mới check-in
        CheckIn checkIn = CheckIn.builder()
                .elderly(elderly)
                .checkInType(type)
                .notes(notes)
                .latitude(lat)
                .longitude(lng)
                .build();
        checkIn = checkInRepository.save(checkIn);

        updateElderlyProfile(elderlyId, lat, lng);

        return checkIn;
    }

    private void updateElderlyProfile(Long elderlyId, BigDecimal lat, BigDecimal lng) {
        elderlyProfileRepository.findByUserId(elderlyId).ifPresent(profile -> {
            profile.setLastCheckinAt(LocalDateTime.now());
            profile.setLastActiveAt(LocalDateTime.now());
            if (lat != null && lng != null) {
                profile.setLatitude(lat);
                profile.setLongitude(lng);
            }
            elderlyProfileRepository.save(profile);
        });
    }

    public List<CheckIn> getCheckInsByElderly(Long elderlyId, int limit) {
        return checkInRepository.findByElderlyIdOrderByCheckedAtDesc(elderlyId, PageRequest.of(0, limit));
    }
}

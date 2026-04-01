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
        if (checkInRepository.findFirstByElderlyIdAndCheckedAtBetweenOrderByCheckedAtDesc(
                elderlyId, startOfDay, endOfDay).isPresent()) {
            throw new RuntimeException("Bạn đã điểm danh hôm nay rồi. Mỗi ngày chỉ điểm danh 1 lần.");
        }

        CheckIn checkIn = CheckIn.builder()
                .elderly(elderly)
                .checkInType(type)
                .notes(notes)
                .latitude(lat)
                .longitude(lng)
                .build();
        checkIn = checkInRepository.save(checkIn);

        elderlyProfileRepository.findByUserId(elderlyId).ifPresent(profile -> {
            profile.setLastCheckinAt(LocalDateTime.now());
            profile.setLastActiveAt(LocalDateTime.now());
            if (lat != null && lng != null) {
                profile.setLatitude(lat);
                profile.setLongitude(lng);
            }
            elderlyProfileRepository.save(profile);
        });

        return checkIn;
    }

    public List<CheckIn> getCheckInsByElderly(Long elderlyId, int limit) {
        return checkInRepository.findByElderlyIdOrderByCheckedAtDesc(elderlyId, PageRequest.of(0, limit));
    }
}

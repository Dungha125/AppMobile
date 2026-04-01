package com.eldercare.repository;

import com.eldercare.model.Alert;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByCaregiverOrderByCreatedAtDesc(User caregiver, org.springframework.data.domain.Pageable pageable);

    List<Alert> findByCaregiverIdOrderByCreatedAtDesc(Long caregiverId, org.springframework.data.domain.Pageable pageable);

    long countByCaregiverIdAndIsReadFalse(Long caregiverId);
}

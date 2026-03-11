package com.eldercare.repository;

import com.eldercare.model.Alert;
import com.eldercare.model.User;
import com.eldercare.model.enums.AlertType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByCaregiverOrderByCreatedAtDesc(User caregiver, Pageable pageable);

    List<Alert> findByCaregiverIdOrderByCreatedAtDesc(Long caregiverId, Pageable pageable);

    /**
     * Fetch alerts với elderly info để tránh N+1 query
     */
    @Query("SELECT a FROM Alert a " +
           "JOIN FETCH a.elderly " +
           "WHERE a.caregiver.id = :caregiverId " +
           "ORDER BY a.createdAt DESC")
    List<Alert> findByCaregiverIdWithElderlyOrderByCreatedAtDesc(
            @Param("caregiverId") Long caregiverId, Pageable pageable);

    long countByCaregiverIdAndIsReadFalse(Long caregiverId);

    long countByIsRead(Boolean isRead);

    long countByAlertType(AlertType alertType);
}

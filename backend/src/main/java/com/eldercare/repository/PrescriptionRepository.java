package com.eldercare.repository;

import com.eldercare.model.Prescription;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByElderlyOrderByCreatedAtDesc(User elderly);

    List<Prescription> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId);

    /**
     * Fetch prescription với medications và schedules để tránh N+1 queries
     */
    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.medications m " +
           "LEFT JOIN FETCH m.schedules " +
           "WHERE p.elderly.id = :elderlyId " +
           "ORDER BY p.createdAt DESC")
    List<Prescription> findByElderlyIdWithMedications(@Param("elderlyId") Long elderlyId);

    /**
     * Fetch một prescription với tất cả relationships
     */
    @Query("SELECT p FROM Prescription p " +
           "LEFT JOIN FETCH p.medications m " +
           "LEFT JOIN FETCH m.schedules " +
           "WHERE p.id = :id")
    Optional<Prescription> findByIdWithMedications(@Param("id") Long id);
}

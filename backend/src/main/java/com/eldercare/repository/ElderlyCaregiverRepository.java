package com.eldercare.repository;

import com.eldercare.model.ElderlyCaregiver;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ElderlyCaregiverRepository extends JpaRepository<ElderlyCaregiver, Long> {

    List<ElderlyCaregiver> findByCaregiver(User caregiver);

    List<ElderlyCaregiver> findByElderly(User elderly);

    Optional<ElderlyCaregiver> findByElderlyAndCaregiver(User elderly, User caregiver);

    boolean existsByElderlyAndCaregiver(User elderly, User caregiver);

    /**
     * Fetch với caregiver để tránh N+1 query
     */
    @Query("SELECT ec FROM ElderlyCaregiver ec " +
           "JOIN FETCH ec.caregiver " +
           "WHERE ec.elderly.id = :elderlyId")
    List<ElderlyCaregiver> findByElderlyIdWithCaregiver(@Param("elderlyId") Long elderlyId);

    /**
     * Fetch với elderly để tránh N+1 query
     */
    @Query("SELECT ec FROM ElderlyCaregiver ec " +
           "JOIN FETCH ec.elderly " +
           "WHERE ec.caregiver.id = :caregiverId")
    List<ElderlyCaregiver> findByCaregiverIdWithElderly(@Param("caregiverId") Long caregiverId);
}

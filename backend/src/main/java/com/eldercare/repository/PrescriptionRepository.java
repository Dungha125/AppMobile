package com.eldercare.repository;

import com.eldercare.model.Prescription;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByElderlyOrderByCreatedAtDesc(User elderly);

    List<Prescription> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId);
}

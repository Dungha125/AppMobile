package com.eldercare.repository;

import com.eldercare.model.ElderlyCaregiver;
import com.eldercare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElderlyCaregiverRepository extends JpaRepository<ElderlyCaregiver, Long> {

    List<ElderlyCaregiver> findByCaregiver(User caregiver);

    List<ElderlyCaregiver> findByElderly(User elderly);

    Optional<ElderlyCaregiver> findByElderlyAndCaregiver(User elderly, User caregiver);

    boolean existsByElderlyAndCaregiver(User elderly, User caregiver);
}

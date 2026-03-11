package com.eldercare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @JsonIgnoreProperties({"medications"})
    private Prescription prescription;

    @Column(nullable = false)
    private String name;

    private String dosage;
    private String unit;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer quantity = 1;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"medication"})
    @org.hibernate.annotations.BatchSize(size = 10)
    @Builder.Default
    private List<MedicationSchedule> schedules = new ArrayList<>();
}

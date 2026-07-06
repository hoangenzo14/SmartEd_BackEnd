// com.example.demo.entity.Subject.java
package com.smarted.ed.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "subject")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<TutorSubject> tutorSubjects;

    @OneToMany(mappedBy = "subject")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Appointment> appointments;
}
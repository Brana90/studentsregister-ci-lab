package com.lab.studentregister.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A project done by a student (mini portfolio tracking). */
@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_student", columnList = "student_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tech_stack")
    private String techStack;

    @Column(name = "repo_url")
    private String repoUrl;

    /** PLANNED | IN_PROGRESS | COMPLETED */
    @Column(nullable = false)
    private String status = "PLANNED";

    @Column(name = "started_date")
    private LocalDate startedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

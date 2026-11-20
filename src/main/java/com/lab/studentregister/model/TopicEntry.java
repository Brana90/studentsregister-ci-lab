package com.lab.studentregister.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A topic the student "signs" as covered on a given day —
 * this is the daily learning progress log.
 */
@Entity
@Table(name = "topic_entries", indexes = {
        @Index(name = "idx_topic_student", columnList = "student_id"),
        @Index(name = "idx_topic_date", columnList = "entry_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "hours_spent")
    private Double hoursSpent;

    /** When the student signed the entry. */
    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt = LocalDateTime.now();
}

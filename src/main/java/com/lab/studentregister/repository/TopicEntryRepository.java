package com.lab.studentregister.repository;

import com.lab.studentregister.model.TopicEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TopicEntryRepository extends JpaRepository<TopicEntry, Long> {

    List<TopicEntry> findByStudentIdOrderByEntryDateDescSignedAtDesc(Long studentId);

    List<TopicEntry> findByEntryDateOrderBySignedAtDesc(LocalDate entryDate);

    List<TopicEntry> findByStudentIdAndEntryDate(Long studentId, LocalDate entryDate);

    long countByStudentId(Long studentId);

    long countByEntryDate(LocalDate entryDate);

    long countByEntryDateBetween(LocalDate from, LocalDate to);

    void deleteByStudentId(Long studentId);

    @Query("select distinct t.entryDate from TopicEntry t " +
           "where t.studentId = ?1 order by t.entryDate desc")
    List<LocalDate> findDistinctDatesByStudent(Long studentId);

    @Query("select coalesce(sum(t.hoursSpent), 0) from TopicEntry t where t.studentId = ?1")
    Double sumHoursByStudent(Long studentId);
}

package com.lab.studentregister.repository;

import com.lab.studentregister.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Project> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
    long countByStudentId(Long studentId);
    long countByStudentIdAndStatus(Long studentId, String status);
    void deleteByStudentId(Long studentId);
}

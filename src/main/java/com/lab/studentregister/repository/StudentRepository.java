package com.lab.studentregister.repository;

import com.lab.studentregister.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository
    extends JpaRepository<Student, Long> {

    List<Student> findByNameContainingIgnoreCase(String name);
    Optional<Student> findByEmail(String email);
    List<Student> findByStatus(String status);
    List<Student> findByCourse(String course);
}
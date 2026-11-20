package com.lab.studentregister.repository;

import com.lab.studentregister.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByStudentId(Long studentId);
    boolean existsByRole(String role);
}

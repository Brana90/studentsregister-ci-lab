package com.lab.studentregister.controller;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Project;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.repository.ProjectRepository;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Projects done by students (portfolio tracking). */
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private static final Set<String> STATUSES =
            Set.of("PLANNED", "IN_PROGRESS", "COMPLETED");

    private final ProjectRepository projectRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserService current;

    public ProjectController(ProjectRepository projectRepository,
                             StudentRepository studentRepository,
                             CurrentUserService current) {
        this.projectRepository = projectRepository;
        this.studentRepository = studentRepository;
        this.current = current;
    }

    public record ProjectRequest(@NotBlank String title,
                                 String description,
                                 String techStack,
                                 String repoUrl,
                                 String status,
                                 Long studentId) {}

    /** Student adds own project; admin can add for any student. */
    @PostMapping
    public ResponseEntity<Project> create(Authentication auth,
                                          @RequestBody ProjectRequest body) {
        AppUser user = current.require(auth);
        Long studentId = current.isAdmin(user) ? body.studentId() : user.getStudentId();
        if (studentId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "studentId is required");
        }
        Project p = new Project();
        p.setStudentId(studentId);
        applyRequest(p, body);
        return ResponseEntity.ok(projectRepository.save(p));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Project>> my(Authentication auth) {
        AppUser user = current.require(auth);
        return ResponseEntity.ok(
                projectRepository.findByStudentIdOrderByCreatedAtDesc(user.getStudentId()));
    }

    /** Admin: all projects (optionally one student's), with student names. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> all(
            @RequestParam(required = false) Long studentId) {
        Map<Long, Student> students = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        List<Project> projects = studentId != null
                ? projectRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                : projectRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> out = projects.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            Student s = students.get(p.getStudentId());
            m.put("id", p.getId());
            m.put("studentId", p.getStudentId());
            m.put("studentName", s != null ? s.getName() : "?");
            m.put("course", s != null ? s.getCourse() : "");
            m.put("title", p.getTitle());
            m.put("description", p.getDescription());
            m.put("techStack", p.getTechStack());
            m.put("repoUrl", p.getRepoUrl());
            m.put("status", p.getStatus());
            m.put("startedDate", p.getStartedDate());
            m.put("completedDate", p.getCompletedDate());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    /** Owner or admin can update. */
    @PutMapping("/{id}")
    public ResponseEntity<Project> update(Authentication auth,
                                          @PathVariable Long id,
                                          @RequestBody ProjectRequest body) {
        AppUser user = current.require(auth);
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));
        current.requireSelfOrAdmin(user, p.getStudentId());
        applyRequest(p, body);
        return ResponseEntity.ok(projectRepository.save(p));
    }

    /** Owner or admin can delete. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(Authentication auth,
                                                      @PathVariable Long id) {
        AppUser user = current.require(auth);
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));
        current.requireSelfOrAdmin(user, p.getStudentId());
        projectRepository.delete(p);
        return ResponseEntity.ok(Map.of("message", "Project deleted"));
    }

    // ------------------------------------------------------------- helpers

    private void applyRequest(Project p, ProjectRequest body) {
        p.setTitle(body.title().trim());
        p.setDescription(body.description());
        p.setTechStack(body.techStack());
        p.setRepoUrl(body.repoUrl());
        String status = body.status() != null
                ? body.status().toUpperCase() : p.getStatus();
        if (!STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status must be one of " + STATUSES);
        }
        // auto-fill lifecycle dates on transitions
        if ("IN_PROGRESS".equals(status) && p.getStartedDate() == null) {
            p.setStartedDate(LocalDate.now());
        }
        if ("COMPLETED".equals(status)) {
            if (p.getStartedDate() == null) p.setStartedDate(LocalDate.now());
            if (p.getCompletedDate() == null) p.setCompletedDate(LocalDate.now());
        } else {
            p.setCompletedDate(null);
        }
        p.setStatus(status);
    }
}

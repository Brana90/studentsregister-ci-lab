package com.lab.studentregister.controller;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.service.CurrentUserService;
import com.lab.studentregister.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;
    private final CurrentUserService current;

    public StudentController(StudentService studentService,
                             CurrentUserService current) {
        this.studentService = studentService;
        this.current = current;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> res = new HashMap<>();
        res.put("status", "UP");
        res.put("service", "student-register");
        return ResponseEntity.ok(res);
    }

    /**
     * Anonymous (the docker HEALTHCHECK) -> harmless status payload.
     * STUDENT -> only their own record. ADMIN -> full roster.
     */
    @GetMapping
    public ResponseEntity<?> getAll(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            Map<String, String> res = new HashMap<>();
            res.put("status", "UP");
            res.put("service", "student-register");
            res.put("message", "login required for data");
            return ResponseEntity.ok(res);
        }
        AppUser user = current.require(auth);
        if (!current.isAdmin(user)) {
            List<Student> self = studentService.getStudentById(user.getStudentId())
                    .map(List::of).orElse(List.of());
            return ResponseEntity.ok(self);
        }
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    /** Admin or the student themself. */
    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(Authentication auth,
                                           @PathVariable Long id) {
        AppUser user = current.require(auth);
        current.requireSelfOrAdmin(user, id);
        return studentService.getStudentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin registers a student; a portal login is auto-created. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Student student) {
        Student saved = studentService.createStudent(student);
        Map<String, Object> res = new HashMap<>();
        res.put("student", saved);
        Map<String, String> login = new HashMap<>();
        login.put("username", saved.getEmail());
        login.put("defaultPassword", StudentService.DEFAULT_STUDENT_PASSWORD);
        res.put("portalLogin", login);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Student> update(@PathVariable Long id,
                                          @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateStudent(id, student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Deleted: " + id);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Student>> search(@RequestParam String name) {
        return ResponseEntity.ok(studentService.searchByName(name));
    }

    @GetMapping("/course/{course}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Student>> byCourse(@PathVariable String course) {
        return ResponseEntity.ok(studentService.getByCourse(course));
    }
}

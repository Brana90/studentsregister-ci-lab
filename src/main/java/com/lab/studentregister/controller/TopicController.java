package com.lab.studentregister.controller;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.model.TopicEntry;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.repository.TopicEntryRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Daily learning log: students "sign" the topics they covered each day.
 */
@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicController {

    private final TopicEntryRepository topicRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserService current;

    public TopicController(TopicEntryRepository topicRepository,
                           StudentRepository studentRepository,
                           CurrentUserService current) {
        this.topicRepository = topicRepository;
        this.studentRepository = studentRepository;
        this.current = current;
    }

    public record TopicRequest(@NotBlank String topic,
                               String description,
                               Double hoursSpent,
                               LocalDate entryDate) {}

    /** Student signs a topic covered (defaults to today). */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TopicEntry> sign(Authentication auth,
                                           @RequestBody TopicRequest body) {
        AppUser user = current.require(auth);
        LocalDate date = body.entryDate() != null ? body.entryDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You cannot sign a future date");
        }
        // no exact duplicate topic on the same day
        boolean duplicate = topicRepository
                .findByStudentIdAndEntryDate(user.getStudentId(), date).stream()
                .anyMatch(t -> t.getTopic().equalsIgnoreCase(body.topic().trim()));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already signed this topic for " + date);
        }
        TopicEntry entry = new TopicEntry();
        entry.setStudentId(user.getStudentId());
        entry.setEntryDate(date);
        entry.setTopic(body.topic().trim());
        entry.setDescription(body.description());
        entry.setHoursSpent(body.hoursSpent());
        return ResponseEntity.ok(topicRepository.save(entry));
    }

    /** Student: own history. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TopicEntry>> my(Authentication auth) {
        AppUser user = current.require(auth);
        return ResponseEntity.ok(topicRepository
                .findByStudentIdOrderByEntryDateDescSignedAtDesc(user.getStudentId()));
    }

    /** Student: streak + totals for the progress header. */
    @GetMapping("/my/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> mySummary(Authentication auth) {
        AppUser user = current.require(auth);
        return ResponseEntity.ok(buildSummary(user.getStudentId()));
    }

    /** Admin: one student's history. */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> byStudent(@PathVariable Long studentId) {
        Map<String, Object> res = buildSummary(studentId);
        res.put("entries", topicRepository
                .findByStudentIdOrderByEntryDateDescSignedAtDesc(studentId));
        return ResponseEntity.ok(res);
    }

    /** Admin: everything signed today, with student names. */
    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> today() {
        Map<Long, Student> students = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        List<Map<String, Object>> out = topicRepository
                .findByEntryDateOrderBySignedAtDesc(LocalDate.now()).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    Student s = students.get(t.getStudentId());
                    m.put("id", t.getId());
                    m.put("studentId", t.getStudentId());
                    m.put("studentName", s != null ? s.getName() : "?");
                    m.put("course", s != null ? s.getCourse() : "");
                    m.put("topic", t.getTopic());
                    m.put("description", t.getDescription());
                    m.put("hoursSpent", t.getHoursSpent());
                    m.put("signedAt", t.getSignedAt());
                    return m;
                }).toList();
        return ResponseEntity.ok(out);
    }

    /** Owner may delete a same-day entry (typo fix); admin may delete any. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(Authentication auth,
                                                      @PathVariable Long id) {
        AppUser user = current.require(auth);
        TopicEntry entry = topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entry not found"));
        if (!current.isAdmin(user)) {
            current.requireSelfOrAdmin(user, entry.getStudentId());
            if (!LocalDate.now().equals(entry.getEntryDate())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only today's entries can be removed");
            }
        }
        topicRepository.delete(entry);
        return ResponseEntity.ok(Map.of("message", "Entry deleted"));
    }

    // ------------------------------------------------------------- helpers

    private Map<String, Object> buildSummary(Long studentId) {
        List<LocalDate> dates = topicRepository.findDistinctDatesByStudent(studentId);
        Map<String, Object> res = new HashMap<>();
        res.put("totalTopics", topicRepository.countByStudentId(studentId));
        Double hours = topicRepository.sumHoursByStudent(studentId);
        res.put("totalHours", hours == null ? 0 : hours);
        res.put("activeDays", dates.size());
        res.put("todaySigned", !dates.isEmpty() && dates.get(0).equals(LocalDate.now()));
        res.put("streak", computeStreak(dates));
        res.put("lastEntryDate", dates.isEmpty() ? null : dates.get(0));
        return res;
    }

    /** Consecutive days with at least one signed topic, ending today/yesterday. */
    static int computeStreak(List<LocalDate> datesDesc) {
        if (datesDesc.isEmpty()) return 0;
        LocalDate today = LocalDate.now();
        LocalDate first = datesDesc.get(0);
        if (!first.equals(today) && !first.equals(today.minusDays(1))) return 0;
        int streak = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            if (datesDesc.get(i).equals(datesDesc.get(i - 1).minusDays(1))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}

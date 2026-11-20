package com.lab.studentregister.controller;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.model.TopicEntry;
import com.lab.studentregister.repository.ProjectRepository;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.repository.TopicEntryRepository;
import com.lab.studentregister.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final StudentRepository studentRepository;
    private final TopicEntryRepository topicRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService current;

    public DashboardController(StudentRepository studentRepository,
                               TopicEntryRepository topicRepository,
                               ProjectRepository projectRepository,
                               CurrentUserService current) {
        this.studentRepository = studentRepository;
        this.topicRepository = topicRepository;
        this.projectRepository = projectRepository;
        this.current = current;
    }

    /** Admin overview: cohort stats + per-student progress table. */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> admin() {
        List<Student> students = studentRepository.findAll();
        LocalDate today = LocalDate.now();

        Map<String, Long> byCourse = new HashMap<>();
        long active = 0;
        for (Student s : students) {
            byCourse.merge(s.getCourse() == null ? "-" : s.getCourse(), 1L, Long::sum);
            if ("active".equalsIgnoreCase(s.getStatus())) active++;
        }

        List<Map<String, Object>> progress = students.stream().map(s -> {
            List<LocalDate> dates = topicRepository.findDistinctDatesByStudent(s.getId());
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", s.getId());
            m.put("name", s.getName());
            m.put("course", s.getCourse());
            m.put("status", s.getStatus());
            m.put("topicsSigned", topicRepository.countByStudentId(s.getId()));
            m.put("activeDays", dates.size());
            m.put("streak", TopicController.computeStreak(dates));
            m.put("lastEntryDate", dates.isEmpty() ? null : dates.get(0));
            m.put("signedToday", !dates.isEmpty() && dates.get(0).equals(today));
            m.put("projects", projectRepository.countByStudentId(s.getId()));
            m.put("projectsCompleted",
                    projectRepository.countByStudentIdAndStatus(s.getId(), "COMPLETED"));
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("totalStudents", (long) students.size());
        res.put("activeStudents", active);
        res.put("byCourse", byCourse);
        res.put("topicsToday", topicRepository.countByEntryDate(today));
        res.put("topicsThisWeek",
                topicRepository.countByEntryDateBetween(today.minusDays(6), today));
        res.put("totalProjects", projectRepository.count());
        res.put("projectsByStatus", Map.of(
                "PLANNED", projectRepository.countByStatus("PLANNED"),
                "IN_PROGRESS", projectRepository.countByStatus("IN_PROGRESS"),
                "COMPLETED", projectRepository.countByStatus("COMPLETED")));
        res.put("studentsProgress", progress);
        return ResponseEntity.ok(res);
    }

    /** Student home: own progress numbers + recent activity. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> my(Authentication auth) {
        AppUser user = current.require(auth);
        Long sid = user.getStudentId();
        List<LocalDate> dates = topicRepository.findDistinctDatesByStudent(sid);
        List<TopicEntry> entries =
                topicRepository.findByStudentIdOrderByEntryDateDescSignedAtDesc(sid);

        Map<String, Object> res = new HashMap<>();
        res.put("totalTopics", topicRepository.countByStudentId(sid));
        Double hours = topicRepository.sumHoursByStudent(sid);
        res.put("totalHours", hours == null ? 0 : hours);
        res.put("activeDays", dates.size());
        res.put("streak", TopicController.computeStreak(dates));
        res.put("todaySigned",
                !dates.isEmpty() && dates.get(0).equals(LocalDate.now()));
        res.put("recentEntries",
                entries.stream().limit(5).toList());
        res.put("projects", projectRepository.countByStudentId(sid));
        res.put("projectsCompleted",
                projectRepository.countByStudentIdAndStatus(sid, "COMPLETED"));
        studentRepository.findById(sid).ifPresent(s -> {
            res.put("name", s.getName());
            res.put("course", s.getCourse());
            res.put("enrolledDate", s.getEnrolledDate());
        });
        return ResponseEntity.ok(res);
    }
}

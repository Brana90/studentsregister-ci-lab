package com.lab.studentregister.service;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.repository.AppUserRepository;
import com.lab.studentregister.repository.ProjectRepository;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.repository.TopicEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    public static final String DEFAULT_STUDENT_PASSWORD = "Student@123";

    private final StudentRepository studentRepository;
    private final AppUserRepository userRepository;
    private final TopicEntryRepository topicRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          AppUserRepository userRepository,
                          TopicEntryRepository topicRepository,
                          ProjectRepository projectRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    /**
     * Creates the student AND a portal login
     * (username = email, password = Student@123).
     */
    @Transactional
    public Student createStudent(Student student) {
        String email = student.getEmail() == null
                ? null : student.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email is required");
        }
        student.setEmail(email);
        if (studentRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A student with this email already exists");
        }
        Student saved = studentRepository.save(student);
        ensureLogin(saved);
        return saved;
    }

    /** Creates the login account for a student if it doesn't exist yet. */
    public boolean ensureLogin(Student student) {
        String username = student.getEmail().trim().toLowerCase();
        if (userRepository.findByUsername(username).isPresent()) {
            return false;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD));
        user.setFullName(student.getName());
        user.setRole("STUDENT");
        user.setStudentId(student.getId());
        userRepository.save(user);
        return true;
    }

    @Transactional
    public Student updateStudent(Long id, Student studentDetails) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found: " + id));

        String oldEmail = student.getEmail();
        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail().trim().toLowerCase());
        student.setPhone(studentDetails.getPhone());
        student.setCourse(studentDetails.getCourse());
        student.setStatus(studentDetails.getStatus());
        Student saved = studentRepository.save(student);

        // keep the login account in sync (username follows the email)
        userRepository.findByStudentId(id).ifPresent(user -> {
            user.setFullName(saved.getName());
            if (!saved.getEmail().equals(oldEmail)) {
                user.setUsername(saved.getEmail());
            }
            user.setActive(!"inactive".equalsIgnoreCase(saved.getStatus()));
            userRepository.save(user);
        });
        return saved;
    }

    /** Deletes the student plus their topics, projects and login. */
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found: " + id);
        }
        topicRepository.deleteByStudentId(id);
        projectRepository.deleteByStudentId(id);
        userRepository.findByStudentId(id).ifPresent(userRepository::delete);
        studentRepository.deleteById(id);
    }

    public List<Student> searchByName(String name) {
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Student> getByCourse(String course) {
        return studentRepository.findByCourse(course);
    }
}

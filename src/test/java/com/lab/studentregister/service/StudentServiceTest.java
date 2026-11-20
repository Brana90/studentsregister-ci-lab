package com.lab.studentregister.service;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.model.Student;
import com.lab.studentregister.repository.AppUserRepository;
import com.lab.studentregister.repository.ProjectRepository;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.repository.TopicEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** StudentService on its own: the repositories are mocks, so no database at all. */
class StudentServiceTest {

    private StudentRepository students;
    private AppUserRepository users;
    private StudentService service;

    @BeforeEach
    void setUp() {
        students = mock(StudentRepository.class);
        users = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("encoded");
        when(students.findByEmail(any())).thenReturn(Optional.empty());
        when(users.findByUsername(any())).thenReturn(Optional.empty());
        when(students.save(any(Student.class))).thenAnswer(call -> {
            Student saved = call.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        service = new StudentService(students, users, mock(TopicEntryRepository.class),
                mock(ProjectRepository.class), encoder);
    }

    private static Student student(String name, String email) {
        Student s = new Student();
        s.setName(name);
        s.setEmail(email);
        s.setPhone("9000000001");
        s.setCourse("DevOps");
        return s;
    }

    @Test
    void emailIsStoredTrimmedAndLowerCase() {
        Student saved = service.createStudent(student("Asha", "  Asha@Example.COM "));
        assertEquals("asha@example.com", saved.getEmail());
    }

    @Test
    void aNewStudentGetsALogin() {
        service.createStudent(student("Ravi", "ravi@example.com"));
        verify(users).save(any(AppUser.class));
    }

    @Test
    void aBlankEmailIsABadRequest() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createStudent(student("Meena", "   ")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(students, never()).save(any());
    }

    @Test
    void aDuplicateEmailIsAConflict() {
        when(students.findByEmail("kumar@example.com")).thenReturn(Optional.of(new Student()));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createStudent(student("Kumar", "kumar@example.com")));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void aNameWithAnApostropheIsAccepted() {
        Student saved = assertDoesNotThrow(
                () -> service.createStudent(student("Anne O'Neil", "anne@example.com")),
                "a real name with an apostrophe must be accepted");
        assertEquals("Anne O'Neil", saved.getName());
    }
}

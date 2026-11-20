package com.lab.studentregister.config;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.repository.AppUserRepository;
import com.lab.studentregister.repository.StudentRepository;
import com.lab.studentregister.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the default admin account on first startup and back-fills
 * portal logins for any students that existed before this feature.
 * Hardcoded credentials are intentional for this lab setup.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seed(AppUserRepository users,
                           StudentRepository students,
                           StudentService studentService,
                           PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByRole("ADMIN")) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("Admin@123"));
                admin.setFullName("System Administrator");
                admin.setRole("ADMIN");
                users.save(admin);
                log.info("admin seeded");
            }

            int created = 0;
            for (var student : students.findAll()) {
                if (student.getEmail() != null
                        && studentService.ensureLogin(student)) {
                    created++;
                }
            }
            if (created > 0) {
                log.info("Back-filled {} student logins", created);
            }
        };
    }
}

package com.lab.studentregister.controller;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.repository.AppUserRepository;
import com.lab.studentregister.security.JwtUtil;
import com.lab.studentregister.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CurrentUserService current;
    private final boolean openAccess;
    private final String openAccessUser;

    public AuthController(AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          CurrentUserService current,
                          @Value("${app.open-access:false}") boolean openAccess,
                          @Value("${app.open-access-user:admin}") String openAccessUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.current = current;
        this.openAccess = openAccess;
        this.openAccessUser = openAccessUser;
    }

    public record LoginRequest(@NotBlank String username,
                               @NotBlank String password) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 6) String newPassword) {}

    public record ResetPasswordRequest(@NotBlank String username,
                                       @NotBlank @Size(min = 6) String newPassword) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body) {
        AppUser user = userRepository
                .findByUsername(body.username().trim().toLowerCase())
                .orElse(null);
        if (user == null
                || !passwordEncoder.matches(body.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account disabled");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(session(user));
    }

    /** Public: tells the frontend whether to skip the sign-in page. */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> mode() {
        return ResponseEntity.ok(Map.of("openAccess", openAccess));
    }

    /** OPEN_ACCESS=true only: a session for the seeded admin, no password. */
    @PostMapping("/open")
    public ResponseEntity<Map<String, Object>> open() {
        if (!openAccess) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        AppUser user = userRepository.findByUsername(openAccessUser)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Open access user '" + openAccessUser + "' is not available yet"));
        return ResponseEntity.ok(session(user));
    }

    private Map<String, Object> session(AppUser user) {
        String token = jwtUtil.generateToken(
                user.getUsername(), user.getRole(),
                user.getFullName(), user.getStudentId());

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("username", user.getUsername());
        res.put("fullName", user.getFullName());
        res.put("role", user.getRole());
        res.put("studentId", user.getStudentId());
        return res;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        AppUser user = current.require(auth);
        Map<String, Object> res = new HashMap<>();
        res.put("username", user.getUsername());
        res.put("fullName", user.getFullName());
        res.put("role", user.getRole());
        res.put("studentId", user.getStudentId());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth, @RequestBody ChangePasswordRequest body) {
        AppUser user = current.require(auth);
        if (!passwordEncoder.matches(body.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Current password is wrong");
        }
        user.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    /** Admin: reset any user's password (e.g. student forgot theirs). */
    @PostMapping("/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest body) {
        AppUser user = userRepository
                .findByUsername(body.username().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(
                Map.of("message", "Password reset for " + user.getUsername()));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> out = userRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("fullName", u.getFullName());
                    m.put("role", u.getRole());
                    m.put("studentId", u.getStudentId());
                    m.put("active", u.isActive());
                    m.put("lastLoginAt", u.getLastLoginAt());
                    return m;
                }).toList();
        return ResponseEntity.ok(out);
    }
}

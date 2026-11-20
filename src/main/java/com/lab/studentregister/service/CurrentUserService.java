package com.lab.studentregister.service;

import com.lab.studentregister.model.AppUser;
import com.lab.studentregister.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the AppUser behind the JWT for the current request. */
@Service
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public CurrentUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser require(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AppUser user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account not found"));
        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account disabled");
        }
        return user;
    }

    public boolean isAdmin(AppUser user) {
        return "ADMIN".equals(user.getRole());
    }

    /** Students may only touch their own data. */
    public void requireSelfOrAdmin(AppUser user, Long studentId) {
        if (!isAdmin(user) && (user.getStudentId() == null
                || !user.getStudentId().equals(studentId))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only access your own records");
        }
    }
}

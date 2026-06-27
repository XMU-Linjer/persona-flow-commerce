package com.personaflow.commerce.auth.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestSecurityController {

    @PostMapping("/api/auth/register")
    String register() {
        return "register";
    }

    @PostMapping("/api/auth/login")
    String login() {
        return "login";
    }

    @GetMapping("/actuator/health")
    String health() {
        return "health";
    }

    @GetMapping("/api/protected")
    String protectedEndpoint() {
        return "protected";
    }

    @GetMapping("/api/admin/check")
    String adminEndpoint() {
        return "admin";
    }

    @GetMapping("/api/users/me")
    String currentUser() {
        return "current-user";
    }

    @PatchMapping("/api/users/me")
    String updateProfile() {
        return "update-profile";
    }

    @PutMapping("/api/users/me/password")
    String changePassword() {
        return "change-password";
    }
}

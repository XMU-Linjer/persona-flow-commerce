package com.personaflow.commerce.auth.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/api/protected")
    String protectedEndpoint() {
        return "protected";
    }

    @GetMapping("/api/admin/check")
    String adminEndpoint() {
        return "admin";
    }
}

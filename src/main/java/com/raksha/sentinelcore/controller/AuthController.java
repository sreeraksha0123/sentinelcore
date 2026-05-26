package com.raksha.sentinelcore.controller;

import com.raksha.sentinelcore.dto.AuthDtos.AuthResponse;
import com.raksha.sentinelcore.dto.AuthDtos.LoginRequest;
import com.raksha.sentinelcore.dto.AuthDtos.RegisterRequest;
import com.raksha.sentinelcore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public auth endpoints — no Bearer token required.
 *
 * <pre>
 * POST /api/auth/register   → {token, type}
 * POST /api/auth/login      → {token, type}
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }
}

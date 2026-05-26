package com.raksha.sentinelcore.service;

import com.raksha.sentinelcore.dto.AuthDtos.AuthResponse;
import com.raksha.sentinelcore.dto.AuthDtos.LoginRequest;
import com.raksha.sentinelcore.dto.AuthDtos.RegisterRequest;
import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.entity.User;
import com.raksha.sentinelcore.exception.Exceptions.EmailAlreadyExistsException;
import com.raksha.sentinelcore.repository.SubscriptionRepository;
import com.raksha.sentinelcore.repository.UserRepository;
import com.raksha.sentinelcore.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles registration, login, and implements {@link UserDetailsService}
 * for Spring Security's authentication chain.
 *
 * <p>On login, the entitlement cache is pre-warmed so the first
 * request to {@code GET /api/entitlements/{userId}} is always a cache HIT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository         userRepo;
    private final SubscriptionRepository subRepo;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final AuthenticationManager  authManager;
    private final EntitlementService     entitlementService;

    // ── Registration ─────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }

        User user = User.builder()
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .build();
        userRepo.save(user);

        // Create FREE subscription for the new user
        Subscription sub = Subscription.builder()
            .user(user)
            .build();
        subRepo.save(sub);

        log.info("New user registered: {} (id={})", user.getEmail(), user.getId());

        String token = jwtService.generate(user.getEmail());
        entitlementService.cacheEntitlements(user.getId());
        return new AuthResponse(token);
    }

    // ── Login ────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepo.findByEmail(req.getEmail()).orElseThrow();
        String token = jwtService.generate(user.getEmail());

        // Pre-warm entitlement cache on every login
        entitlementService.cacheEntitlements(user.getId());

        log.info("User logged in: {} (id={})", user.getEmail(), user.getId());
        return new AuthResponse(token);
    }

    // ── UserDetailsService ───────────────────────────────────

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

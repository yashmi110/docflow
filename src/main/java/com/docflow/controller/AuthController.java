package com.docflow.controller;

import com.docflow.dto.auth.*;
import com.docflow.service.Auth0Service;
import com.docflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final Auth0Service auth0Service;

    // ========== Traditional Email/Password Auth ==========
    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ========== Auth0 Authentication ==========
    
    @PostMapping("/auth0/signup")
    public ResponseEntity<AuthResponse> auth0Signup(@Valid @RequestBody Auth0SignupRequest request) {
        AuthResponse response = auth0Service.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth0/login")
    public ResponseEntity<AuthResponse> auth0Login(@Valid @RequestBody Auth0LoginRequest request) {
        AuthResponse response = auth0Service.login(request);
        return ResponseEntity.ok(response);
    }
}

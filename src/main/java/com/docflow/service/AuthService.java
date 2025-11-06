package com.docflow.service;

import com.docflow.domain.entity.Role;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.RoleName;
import com.docflow.dto.auth.AuthResponse;
import com.docflow.dto.auth.LoginRequest;
import com.docflow.dto.auth.SignupRequest;
import com.docflow.repository.RoleRepository;
import com.docflow.repository.UserRepository;
import com.docflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Lazy AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Get EMPLOYEE role by default
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("Default role EMPLOYEE not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        // Generate JWT
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_EMPLOYEE")
                .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String googleSub) {
        return userRepository.findByGoogleSubWithRoles(googleSub)
                .orElseGet(() -> {
                    // Check if user exists by email
                    return userRepository.findByEmailWithRoles(email)
                            .map(existingUser -> {
                                // Link Google account to existing user
                                existingUser.setGoogleSub(googleSub);
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                // Create new user
                                Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                                        .orElseThrow(() -> new IllegalStateException("Default role EMPLOYEE not found"));

                                Set<Role> roles = new HashSet<>();
                                roles.add(employeeRole);

                                User newUser = User.builder()
                                        .name(name)
                                        .email(email)
                                        .googleSub(googleSub)
                                        .enabled(true)
                                        .roles(roles)
                                        .build();

                                return userRepository.save(newUser);
                            });
                });
    }

    public AuthResponse createAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }
}

package com.pk.junkchat_backend.controller;

import com.pk.junkchat_backend.config.JwtUtil;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.service.EmailService;
import com.pk.junkchat_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            logger.info("Registering user: {}", user.getUsername());
            return ResponseEntity.ok(userService.registerUser(user));
        } catch (Exception ex) {
            logger.error("Registration failed for user: {} - {}", user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Registration failed: " + ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            logger.info("Login attempt for username: {}", user.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            String jwt = jwtUtil.generateToken(authentication.getName());
            logger.info("Login successful for username: {}", user.getUsername());
            return ResponseEntity.ok(new JwtResponse(jwt));
        } catch (AuthenticationException ex) {
            logger.error("Authentication failed for username: {} - {}", user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during login for username: {} - {}", user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login: " + ex.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            logger.info("Password reset request for email: {}", request.getEmail());
            User user = userService.findByIdentifier(request.getEmail());
            String token = jwtUtil.generateToken(user.getUsername());
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            logger.info("Password reset email sent to: {}", user.getEmail());
            return ResponseEntity.ok("Password reset email sent successfully");
        } catch (UsernameNotFoundException ex) {
            logger.warn("User not found for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found for email: " + request.getEmail());
        } catch (Exception ex) {
            logger.error("Error sending password reset email for: {} - {}", request.getEmail(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending password reset email: " + ex.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            logger.info("Password reset attempt with token");
            String username = jwtUtil.getUsernameFromToken(request.getToken());
            User user = userService.findByIdentifier(username);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userService.updateUser(user.getId(), user);
            logger.info("Password reset successful for username: {}", username);
            return ResponseEntity.ok("Password reset successfully");
        } catch (UsernameNotFoundException ex) {
            logger.warn("User not found for token");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception ex) {
            logger.error("Password reset failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password reset failed: " + ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        logger.info("User logged out");
        return ResponseEntity.ok("Logged out successfully");
    }

    static class JwtResponse {
        private final String jwt;

        public JwtResponse(String token) {
            this.jwt = token;
        }

        public String getToken() {
            return jwt;
        }
    }

    static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    static class ResetPasswordRequest {
        private String token;
        private String password;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
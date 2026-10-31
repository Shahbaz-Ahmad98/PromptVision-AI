
package com.ai.SpringAiDemo.auth;

import com.ai.SpringAiDemo.user.AppUser;
import com.ai.SpringAiDemo.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // REGISTER USER
    public void register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);

        // Store BCrypt-hashed password
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setRole("USER");

        userRepository.save(user);
    }

    // LOGIN USER
    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Find user in PostgreSQL
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password"
                    )
                );

        // Verify password using BCrypt
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // Generate JWT
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}

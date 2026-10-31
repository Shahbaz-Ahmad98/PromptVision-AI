package com.ai.SpringAiDemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.List;

@Configuration
public class SecurityConfig {

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==========================================
    // JWT SECRET KEY
    // ==========================================

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String secret) {

        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes"
            );
        }

        return new SecretKeySpec(
                keyBytes,
                "HmacSHA256"
        );
    }

    // ==========================================
    // JWT ENCODER
    // ==========================================

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    // ==========================================
    // JWT DECODER
    // ==========================================

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "spring-ai-studio"
                )
        );

        return decoder;
    }

    // ==========================================
    // CORS CONFIGURATION
    // ==========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Allow React frontend
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        // Allow API request methods
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allow JWT and JSON headers
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // ==========================================
    // SPRING SECURITY FILTER CHAIN
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Disable CSRF for stateless JWT APIs
                .csrf(csrf -> csrf.disable())

                // Use CORS configuration above
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // No server-side HTTP sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // API access rules
                .authorizeHttpRequests(auth -> auth

                        // Public registration and login
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // All other endpoints require JWT
                        .anyRequest().authenticated()
                )

                // Validate JWT Bearer tokens
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {})
                );

        return http.build();
    }
}

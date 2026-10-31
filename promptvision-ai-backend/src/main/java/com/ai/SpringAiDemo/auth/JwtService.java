
package com.ai.SpringAiDemo.auth;

import com.ai.SpringAiDemo.user.AppUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-minutes:60}")
            long expirationMinutes) {

        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AppUser user) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("spring-ai-studio")
                .issuedAt(now)
                .expiresAt(
                    now.plusSeconds(expirationMinutes * 60)
                )
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}

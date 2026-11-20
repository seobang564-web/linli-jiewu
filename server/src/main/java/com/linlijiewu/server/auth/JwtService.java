package com.linlijiewu.server.auth;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.linlijiewu.server.common.BusinessException;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiresIn;

    public JwtService(Environment environment) {
        try {
            byte[] bytes = Base64.getDecoder().decode(environment.getProperty("linli.jwt.secret", ""));
            if (bytes.length < 32) { throw new IllegalArgumentException(); }
            key = Keys.hmacShaKeyFor(bytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET 必须是至少 32 字节随机值的 Base64 编码", exception);
        }
        expiresIn = environment.getProperty("linli.jwt.expires-in-seconds", Long.class, 604800L);
        if (expiresIn <= 0) { throw new IllegalStateException("Token 有效期必须大于零"); }
    }

    public String issue(long userId) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("linli-jiewu").subject(Long.toString(userId))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expiresIn)))
                .signWith(key).compact();
    }

    public long verify(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).requireIssuer("linli-jiewu")
                    .build().parseSignedClaims(token).getPayload();
            long id = Long.parseLong(claims.getSubject());
            if (id <= 0 || claims.getExpiration() == null) { throw new IllegalArgumentException(); }
            return id;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            throw BusinessException.unauthorized();
        }
    }

    public long expiresIn() { return expiresIn; }
}

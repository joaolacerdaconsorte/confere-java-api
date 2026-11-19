package br.com.confere.security;

import br.com.confere.domain.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 bytes (HS256)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .claim("role", usuario.getPerfil().name())
                .claim("municipio", usuario.getMunicipio().getCodIbge())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return properties.getExpirationSeconds();
    }
}

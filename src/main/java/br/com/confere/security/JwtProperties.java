package br.com.confere.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "confere.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private String issuer = "confere-java";
    private String audience = "confere-app";
    private long expirationSeconds = 28800;
}

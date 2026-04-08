package com.exchange.account.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "exchange.jwt")
public class JwtProperties {
    private String secret;
    private long expiryMs = 86_400_000L; // 24h default
}

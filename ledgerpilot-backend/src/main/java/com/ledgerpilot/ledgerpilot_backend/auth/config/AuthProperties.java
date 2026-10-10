package com.ledgerpilot.ledgerpilot_backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(String jwtSecret, String issuer, String audience, long accessTokenMinutes,
                             long refreshTokenDays,
                             boolean cookieSecure, String cookieSameSite, String frontendBaseUrl) {
}

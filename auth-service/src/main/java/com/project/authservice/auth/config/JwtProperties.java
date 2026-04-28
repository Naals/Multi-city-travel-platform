package com.project.authservice.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtProperties {
    private String secret;
    private long accessTokenExpiryMs  = 900_000L;       // 15 min
    private long refreshTokenExpirySec = 604_800L;      // 7 days
}
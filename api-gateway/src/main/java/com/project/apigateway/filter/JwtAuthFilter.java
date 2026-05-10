package com.project.apigateway.filter;

import com.project.apigateway.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class JwtAuthFilter extends
        AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtProperties jwtProperties;

    public JwtAuthFilter(JwtProperties jwtProperties) {
        super(Config.class);
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);


            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return reject(exchange,
                        "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = parseToken(token);
                String userId = claims.getSubject();
                String role   = claims.get("role", String.class);
                String email  = claims.get("email", String.class);


                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id",   userId)
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-User-Email", email != null ? email : "")
                        .build();

                return chain.filter(exchange.mutate()
                        .request(mutatedRequest).build());

            } catch (ExpiredJwtException ex) {
                log.debug("Expired JWT from {}", request.getRemoteAddress());
                return reject(exchange,
                        "Token has expired");

            } catch (JwtException ex) {
                log.debug("Invalid JWT: {}", ex.getMessage());
                return reject(exchange,
                        "Invalid token");
            }
        };
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> reject(ServerWebExchange exchange,
                              String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), message
        );

        var buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static class Config {

    }
}

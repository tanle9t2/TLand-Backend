package com.tanle.tland.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class JwtClaimsFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    if (auth.getPrincipal() instanceof Jwt jwt) {
                        String username = jwt.getClaimAsString("preferred_username");

                        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("tland-react");
                        List<String> roles = clientAccess != null
                                ? (List<String>) clientAccess.get("roles")
                                : List.of();
                        String userId = jwt.getClaimAsString("sub");
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Username", username)
                                .header("X-UserId", userId)
                                .header("X-Roles", String.join(",", roles))
                                .build();

                        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                        return chain.filter(mutatedExchange);
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

}

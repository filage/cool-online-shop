package com.coolonlineshop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = removeUserHeaders(exchange);

        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuthentication -> addUserHeaders(sanitizedExchange, jwtAuthentication))
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private ServerWebExchange removeUserHeaders(ServerWebExchange exchange) {
        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_EMAIL_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();

        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange addUserHeaders(
            ServerWebExchange exchange,
            JwtAuthenticationToken jwtAuthentication
    ) {
        var jwt = jwtAuthentication.getToken();

        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(USER_ID_HEADER, jwt.getSubject());
                    headers.set(USER_EMAIL_HEADER, jwt.getClaimAsString("email"));
                    headers.set(USER_ROLE_HEADER, jwt.getClaimAsString("role"));
                })
                .build();

        return exchange.mutate().request(request).build();
    }
}

package com.mri.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    public static final String USER_HEADER = "X-Authenticated-User";
    public static final String ROLES_HEADER = "X-Authenticated-Roles";

    private final GatewayTokenValidator validator;

    public GatewayAuthFilter(GatewayTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        return validator.authorize(path, exchange.getRequest().getMethod(), authorization)
                .flatMap(result -> {
                    if (result.status() == AuthorizationStatus.UNAUTHENTICATED) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
                    }
                    if (result.status() == AuthorizationStatus.FORBIDDEN) {
                        return reject(exchange, HttpStatus.FORBIDDEN, "当前账号没有执行此操作的权限");
                    }
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove(USER_HEADER);
                                headers.remove(ROLES_HEADER);
                                if (result.claims() != null) {
                                    headers.set(USER_HEADER, result.claims().subject());
                                    headers.set(ROLES_HEADER, result.claims().roles().stream().sorted().collect(Collectors.joining(",")));
                                }
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] bytes = ("{\"success\":false,\"code\":\"" + status.name() + "\",\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

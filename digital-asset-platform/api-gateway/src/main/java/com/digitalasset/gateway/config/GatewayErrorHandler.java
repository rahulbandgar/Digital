package com.digitalasset.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Map;

@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;
        String errorCode;

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof ConnectException || ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status = HttpStatus.BAD_GATEWAY;
            message = "Upstream service is unavailable. Please try again shortly.";
            errorCode = "SERVICE_UNAVAILABLE";
            log.warn("Gateway: upstream connection refused for {}", exchange.getRequest().getPath());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            errorCode = "HTTP_" + status.value();
            if (status.is4xxClientError()) {
                log.debug("Gateway 4xx {}: {}", status, message);
            } else {
                log.warn("Gateway error {}: {}", status, message);
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected gateway error occurred";
            errorCode = "GATEWAY_ERROR";
            log.error("Gateway unexpected error for {}", exchange.getRequest().getPath(), ex);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "message", message,
                "errorCode", errorCode
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"Gateway error\",\"errorCode\":\"GATEWAY_ERROR\"}").getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

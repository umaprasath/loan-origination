package com.loanorigination.apigateway.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
public class WelcomeController {
    
    @Bean
    public RouterFunction<ServerResponse> welcomeRoute() {
        return route()
            .GET("/", request -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "service", "Loan Origination System - API Gateway",
                    "version", "1.0.0",
                    "status", "running",
                    "endpoints", Map.of(
                        "health", "/actuator/health",
                        "creditCheck", "/api/credit/check",
                        "orchestratorSwagger", "http://localhost:8081/swagger-ui/index.html",
                        "decisionEngineSwagger", "http://localhost:8082/swagger-ui/index.html"
                    ),
                    "description", "API Gateway for the Loan Origination System. Use /api/credit/check to submit loan applications."
                )))
            .build();
    }
}


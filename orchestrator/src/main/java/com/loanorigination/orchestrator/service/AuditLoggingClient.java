package com.loanorigination.orchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AuditLoggingClient {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLoggingClient.class);
    
    private final WebClient.Builder webClientBuilder;
    
    public AuditLoggingClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    private static final String AUDIT_SERVICE_URL = "http://localhost:8085/api/audit";
    
    public void logEvent(String requestId, String action, Object details) {
        WebClient webClient = webClientBuilder.baseUrl(AUDIT_SERVICE_URL).build();
        
        webClient.post()
                .uri("/log")
                .bodyValue(new AuditLogRequest(requestId, "ORCHESTRATOR", action, details))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                    result -> log.debug("Audit log created for request: {}", requestId),
                    error -> log.error("Error creating audit log: {}", error.getMessage())
                );
    }
    
    private record AuditLogRequest(String requestId, String serviceName, String action, Object details) {}
}


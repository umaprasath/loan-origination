package com.loanorigination.mcp.service;

import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.dto.CreditResponse;
import com.loanorigination.common.dto.DecisionReasoning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class McpService {
    
    private static final Logger log = LoggerFactory.getLogger(McpService.class);
    
    private final WebClient webClient;
    private final String orchestratorUrl;
    private final String decisionEngineUrl;
    private final String auditLoggingUrl;
    
    public McpService(
            WebClient.Builder webClientBuilder,
            @Value("${services.orchestrator.url}") String orchestratorUrl,
            @Value("${services.decision-engine.url}") String decisionEngineUrl,
            @Value("${services.audit-logging.url}") String auditLoggingUrl) {
        this.webClient = webClientBuilder.build();
        this.orchestratorUrl = orchestratorUrl;
        this.decisionEngineUrl = decisionEngineUrl;
        this.auditLoggingUrl = auditLoggingUrl;
    }
    
    public Mono<CreditResponse> performCreditCheck(CreditRequest request) {
        log.info("MCP: Performing credit check for SSN: {}", request.getSsn());
        return webClient.post()
                .uri(orchestratorUrl + "/credit/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreditResponse.class)
                .doOnSuccess(response -> log.info("MCP: Credit check completed - Status: {}", response.getStatus()))
                .doOnError(error -> log.error("MCP: Credit check failed", error));
    }
    
    public Mono<DecisionReasoning> getDecisionReasoning(String requestId) {
        log.info("MCP: Fetching decision reasoning for requestId: {}", requestId);
        return webClient.get()
                .uri(decisionEngineUrl + "/api/decision/reasoning/{requestId}", requestId)
                .retrieve()
                .bodyToMono(DecisionReasoning.class)
                .doOnSuccess(reasoning -> log.info("MCP: Reasoning retrieved successfully"))
                .doOnError(error -> log.error("MCP: Failed to retrieve reasoning", error));
    }
    
    public Mono<Map<String, Object>> checkLlmStatus() {
        log.info("MCP: Checking LLM status");
        return webClient.get()
                .uri(decisionEngineUrl + "/api/llm/status")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(status -> log.info("MCP: LLM status retrieved"))
                .doOnError(error -> log.error("MCP: Failed to check LLM status", error));
    }
    
    public Mono<Void> logAuditEvent(String requestId, String serviceName, String action, Object details) {
        log.info("MCP: Logging audit event - RequestId: {}, Service: {}, Action: {}", requestId, serviceName, action);
        return webClient.post()
                .uri(auditLoggingUrl + "/api/audit/log")
                .bodyValue(Map.of(
                        "requestId", requestId,
                        "serviceName", serviceName,
                        "action", action,
                        "details", details
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("MCP: Audit event logged successfully"))
                .doOnError(error -> log.error("MCP: Failed to log audit event", error));
    }
}


package com.loanorigination.sms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.dto.CreditResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OrchestratorClient {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrchestratorClient(@Value("${orchestrator.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<CreditResponse> submitCreditRequest(CreditRequest request) {
        return webClient.post()
                .uri("/api/credit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreditResponse.class)
                .doOnError(e -> log.error("Failed to call orchestrator: {}", e.getMessage()));
    }

    public String summarize(CreditResponse response) {
        String decision = response.getDecision() != null ? response.getDecision() : response.getStatus();
        String reason = response.getDecisionReason() != null ? response.getDecisionReason() : response.getReason();
        String score = response.getCreditScore() != null ? String.valueOf(response.getCreditScore().intValue()) : "---";
        return String.format("Decision: %s | Score: %s | %s", decision, score, reason != null ? reason : "No reason");
    }
}



package com.loanorigination.orchestrator.service;

import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.common.dto.DecisionResult;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DecisionEngineClient {
    
    private final WebClient.Builder webClientBuilder;
    
    public DecisionEngineClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    private static final String DECISION_ENGINE_URL = "http://localhost:8082/api/decision";
    
    public DecisionResult getDecision(DecisionRequest request) {
        WebClient webClient = webClientBuilder.baseUrl(DECISION_ENGINE_URL).build();
        
        return webClient.post()
                .uri("/evaluate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DecisionResult.class)
                .block();
    }
}


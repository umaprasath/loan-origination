package com.loanorigination.orchestrator.service;

import com.loanorigination.common.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrchestrationService {
    
    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    
    private final WebClient.Builder webClientBuilder;
    private final DecisionEngineClient decisionEngineClient;
    private final AuditLoggingClient auditLoggingClient;
    
    public OrchestrationService(WebClient.Builder webClientBuilder, 
                               DecisionEngineClient decisionEngineClient,
                               AuditLoggingClient auditLoggingClient) {
        this.webClientBuilder = webClientBuilder;
        this.decisionEngineClient = decisionEngineClient;
        this.auditLoggingClient = auditLoggingClient;
    }
    
    private static final String EXPERIAN_SERVICE_URL = "http://localhost:8083/api/experian";
    private static final String EQUIFAX_SERVICE_URL = "http://localhost:8084/api/equifax";
    
    public CreditResponse processCreditCheck(CreditRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Processing credit check request: {}", requestId);
        
        // Parallel scatter-gather pattern for credit bureau calls
        Mono<BureauResponse> experianMono = callExperian(request, requestId);
        Mono<BureauResponse> equifaxMono = callEquifax(request, requestId);
        
        // Wait for both responses in parallel
        Tuple2<BureauResponse, BureauResponse> results = Mono.zip(experianMono, equifaxMono)
                .block();
        
        BureauResponse experianResponse = results.getT1();
        BureauResponse equifaxResponse = results.getT2();
        
               // Call Decision Engine
               DecisionRequest decisionRequest = new DecisionRequest(
                       requestId,
                       request.getLoanAmount(),
                       List.of(experianResponse, equifaxResponse)
               );
               // Add financial data for LLM evaluation
               decisionRequest.setAnnualIncome(request.getAnnualIncome());
               decisionRequest.setTotalDebt(request.getTotalDebt());
               decisionRequest.setMonthlyCashflow(request.getMonthlyCashflow());
               if (request.getApplicantAge() != null && request.getApplicantAge() > 0) {
                   decisionRequest.setApplicantAge(BigDecimal.valueOf(request.getApplicantAge()));
               }
        
        DecisionResult decision = decisionEngineClient.getDecision(decisionRequest);
        
        // Audit logging (async)
        auditLoggingClient.logEvent(requestId, "CREDIT_CHECK", request);
        
        // Build response
        CreditResponse response = new CreditResponse(
                requestId,
                decision.getDecision(),
                decision.getCreditScore(),
                request.getLoanAmount(),
                decision.getReason(),
                LocalDateTime.now(),
                experianResponse,
                equifaxResponse
        );
        
        // Add reasoning if available
        if (decision.getReasoning() != null) {
            response.setReasoning(decision.getReasoning());
        }
        
        return response;
    }
    
    private Mono<BureauResponse> callExperian(CreditRequest request, String requestId) {
        WebClient webClient = webClientBuilder.baseUrl(EXPERIAN_SERVICE_URL).build();
        
        return webClient.post()
                .uri("/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BureauResponse.class)
                .doOnError(error -> {
                    log.error("Error calling Experian service: {}", error.getMessage());
                })
                .onErrorReturn(createErrorResponse("EXPERIAN", requestId));
    }
    
    private Mono<BureauResponse> callEquifax(CreditRequest request, String requestId) {
        WebClient webClient = webClientBuilder.baseUrl(EQUIFAX_SERVICE_URL).build();
        
        return webClient.post()
                .uri("/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BureauResponse.class)
                .doOnError(error -> {
                    log.error("Error calling Equifax service: {}", error.getMessage());
                })
                .onErrorReturn(createErrorResponse("EQUIFAX", requestId));
    }
    
    private BureauResponse createErrorResponse(String bureauName, String requestId) {
        return new BureauResponse(
                bureauName,
                null, // creditScore
                "FAILED",
                "Service unavailable",
                LocalDateTime.now()
        );
    }
}


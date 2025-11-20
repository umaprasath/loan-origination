package com.loanorigination.decisionengine.service;

import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.common.dto.DecisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Hybrid decision service that combines rule-based and LLM-based decision making
 */
@Service
public class HybridDecisionService {
    
    private static final Logger log = LoggerFactory.getLogger(HybridDecisionService.class);
    
    private final DecisionService ruleBasedService;
    private final LLMDecisionService llmService;
    private final boolean useLLM;
    private final String decisionMode; // "rules", "llm", "hybrid"
    
    public HybridDecisionService(
            DecisionService ruleBasedService,
            LLMDecisionService llmService,
            @Value("${decision.mode:rules}") String decisionMode,
            @Value("${llm.enabled:false}") boolean llmEnabled) {
        this.ruleBasedService = ruleBasedService;
        this.llmService = llmService;
        this.decisionMode = decisionMode;
        this.useLLM = llmEnabled && llmService.isEnabled();
        
        log.info("Hybrid Decision Service initialized with mode: {} (LLM enabled: {})", 
                decisionMode, useLLM);
    }
    
    /**
     * Evaluates a loan decision using the configured decision mode
     */
    public DecisionResult evaluate(DecisionRequest request) {
        BigDecimal averageScore = calculateAverageScore(request);
        
        switch (decisionMode.toLowerCase()) {
            case "llm":
                if (!useLLM) {
                    log.warn("LLM mode requested but LLM is not enabled. Falling back to rules.");
                    return ruleBasedService.evaluate(request);
                }
                return llmService.evaluateWithLLM(request, averageScore);
                
            case "hybrid":
                return evaluateHybrid(request, averageScore);
                
            case "rules":
            default:
                return ruleBasedService.evaluate(request);
        }
    }
    
    /**
     * Hybrid evaluation: Uses both rule-based and LLM, then combines results
     */
    private DecisionResult evaluateHybrid(DecisionRequest request, BigDecimal averageScore) {
        // Get rule-based decision
        DecisionResult ruleResult = ruleBasedService.evaluate(request);
        
        if (!useLLM) {
            log.debug("LLM not available for hybrid mode. Using rule-based decision only.");
            return ruleResult;
        }
        
        try {
            // Get LLM decision
            DecisionResult llmResult = llmService.evaluateWithLLM(request, averageScore);
            
            // Combine decisions (both must approve for approval)
            DecisionResult finalResult = new DecisionResult();
            finalResult.setRequestId(request.getRequestId());
            finalResult.setCreditScore(averageScore);
            finalResult.setTimestamp(ruleResult.getTimestamp());
            
            if ("APPROVED".equals(ruleResult.getDecision()) && 
                "APPROVED".equals(llmResult.getDecision())) {
                finalResult.setDecision("APPROVED");
                finalResult.setReason("Both rule-based and LLM evaluations approved the loan");
            } else {
                finalResult.setDecision("REJECTED");
                StringBuilder reason = new StringBuilder("Loan rejected: ");
                if (!"APPROVED".equals(ruleResult.getDecision())) {
                    reason.append("Rule-based evaluation failed. ");
                }
                if (!"APPROVED".equals(llmResult.getDecision())) {
                    reason.append("LLM evaluation failed. ");
                }
                reason.append("Rule reason: ").append(ruleResult.getReason());
                reason.append(" LLM reason: ").append(llmResult.getReason());
                finalResult.setReason(reason.toString());
            }
            
            log.info("Hybrid decision for request {}: {} (Rules: {}, LLM: {})",
                    request.getRequestId(),
                    finalResult.getDecision(),
                    ruleResult.getDecision(),
                    llmResult.getDecision());
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("LLM evaluation failed in hybrid mode. Using rule-based decision only: {}", 
                    e.getMessage());
            return ruleResult; // Fallback to rule-based
        }
    }
    
    private BigDecimal calculateAverageScore(DecisionRequest request) {
        return request.getBureauResponses().stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()) && r.getCreditScore() != null)
                .map(r -> r.getCreditScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(
                        request.getBureauResponses().stream()
                                .filter(r -> "SUCCESS".equals(r.getStatus()) && r.getCreditScore() != null)
                                .count()), 2, java.math.RoundingMode.HALF_UP);
    }
}



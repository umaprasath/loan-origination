package com.loanorigination.decisionengine.service;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.common.dto.DecisionReasoning;
import com.loanorigination.common.dto.DecisionResult;
import com.loanorigination.decisionengine.entity.Decision;
import com.loanorigination.decisionengine.entity.RuleConfiguration;
import com.loanorigination.decisionengine.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DecisionService {
    
    private static final Logger log = LoggerFactory.getLogger(DecisionService.class);
    
    private final DecisionRepository decisionRepository;
    private final ReasoningService reasoningService;
    private final RuleConfigurationService ruleConfigurationService;
    
    public DecisionService(DecisionRepository decisionRepository, 
                          ReasoningService reasoningService,
                          RuleConfigurationService ruleConfigurationService) {
        this.decisionRepository = decisionRepository;
        this.reasoningService = reasoningService;
        this.ruleConfigurationService = ruleConfigurationService;
    }
    
    @Transactional
    public DecisionResult evaluate(DecisionRequest request) {
        log.info("Evaluating decision for request: {}", request.getRequestId());
        
        // Check cache first
        Decision cachedDecision = getCachedDecision(request.getRequestId());
        if (cachedDecision != null) {
            log.info("Returning cached decision for request: {}", request.getRequestId());
            DecisionResult result = mapToDecisionResult(cachedDecision);
            // Regenerate reasoning for cached decisions to ensure consistency
            BigDecimal averageScore = cachedDecision.getCreditScore();
            DecisionReasoning reasoning = reasoningService.generateReasoning(request, averageScore, cachedDecision.getDecision());
            result.setReasoning(reasoning);
            return result;
        }
        
        // Calculate average credit score from bureau responses
        BigDecimal averageScore = calculateAverageScore(request.getBureauResponses());
        
        // Decision logic using dynamic rules
        String decision = "REJECTED";
        String reason = evaluateRules(request, averageScore);
        
        if (reason == null || reason.isEmpty()) {
            decision = "APPROVED";
            reason = "All rules passed";
        }
        
        // Generate detailed reasoning
        DecisionReasoning reasoning = reasoningService.generateReasoning(request, averageScore, decision);
        
        // Persist decision
        Decision decisionEntity = new Decision();
        decisionEntity.setRequestId(request.getRequestId());
        decisionEntity.setDecision(decision);
        decisionEntity.setCreditScore(averageScore);
        decisionEntity.setLoanAmount(request.getLoanAmount());
        decisionEntity.setReason(reason);
        decisionEntity.setTimestamp(LocalDateTime.now());
        
        decisionRepository.save(decisionEntity);
        
        log.info("Decision made for request {}: {}", request.getRequestId(), decision);
        log.debug("Decision reasoning: {}", reasoning.getSummary());
        
        DecisionResult result = mapToDecisionResult(decisionEntity);
        result.setReasoning(reasoning);
        
        return result;
    }
    
    @Cacheable(value = "creditDecisions", key = "#requestId")
    public Decision getCachedDecision(String requestId) {
        return decisionRepository.findByRequestId(requestId).orElse(null);
    }
    
    /**
     * Retrieves detailed reasoning for a decision by request ID
     */
    public DecisionReasoning getReasoning(String requestId) {
        Decision decision = decisionRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Decision not found for request: " + requestId));
        
        // Reconstruct the request from stored decision
        // Note: In a real system, you might want to store the original request
        // For now, we'll need to fetch bureau responses from elsewhere or store them
        log.warn("Reasoning retrieval requires original request data. Returning basic reasoning.");
        
        // For now, return a basic reasoning structure
        DecisionReasoning reasoning = new DecisionReasoning();
        reasoning.setSummary(decision.getReason());
        reasoning.setDecisionPath("Decision stored at: " + decision.getTimestamp());
        
        return reasoning;
    }
    
    private BigDecimal calculateAverageScore(List<BureauResponse> responses) {
        List<BigDecimal> validScores = responses.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()) && r.getCreditScore() != null)
                .map(BureauResponse::getCreditScore)
                .collect(Collectors.toList());
        
        if (validScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = validScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(new BigDecimal(validScores.size()), 2, RoundingMode.HALF_UP);
    }
    
    private DecisionResult mapToDecisionResult(Decision decision) {
        DecisionResult result = new DecisionResult();
        result.setRequestId(decision.getRequestId());
        result.setDecision(decision.getDecision());
        result.setCreditScore(decision.getCreditScore());
        result.setReason(decision.getReason());
        result.setTimestamp(decision.getTimestamp());
        return result;
    }
    
    /**
     * Evaluates all active rules and returns failure reason if any rule fails
     * Returns null if all rules pass
     */
    private String evaluateRules(DecisionRequest request, BigDecimal averageScore) {
        List<RuleConfiguration> activeRules = ruleConfigurationService.getAllActiveRules();
        
        for (RuleConfiguration rule : activeRules) {
            boolean passed = evaluateRule(rule, request, averageScore);
            if (!passed) {
                String failureMessage = rule.getFailureMessage();
                if (failureMessage == null || failureMessage.isEmpty()) {
                    return String.format("Rule '%s' failed: %s", rule.getRuleName(), rule.getDescription());
                }
                return failureMessage;
            }
        }
        
        return null; // All rules passed
    }
    
    /**
     * Evaluates a single rule
     */
    private boolean evaluateRule(RuleConfiguration rule, DecisionRequest request, BigDecimal averageScore) {
        BigDecimal threshold = rule.getThresholdValue();
        String operator = rule.getOperator();
        BigDecimal valueToCompare = null;
        
        // Determine which value to compare based on rule type
        switch (rule.getRuleType()) {
            case "CREDIT_SCORE":
                valueToCompare = averageScore;
                break;
            case "LOAN_AMOUNT":
                valueToCompare = request.getLoanAmount();
                break;
            case "BUREAU_RESPONSE":
                // For bureau response, check if at least one response is successful
                long successCount = request.getBureauResponses().stream()
                    .filter(r -> "SUCCESS".equals(r.getStatus()))
                    .count();
                valueToCompare = BigDecimal.valueOf(successCount);
                break;
            case "AGE_LIMIT":
                valueToCompare = request.getApplicantAge();
                break;    
            default:
                log.warn("Unknown rule type: {}", rule.getRuleType());
                return true; // Skip unknown rule types
        }
        
        if (valueToCompare == null) {
            log.warn("Rule {} expects value for type {} but it was not provided", rule.getRuleName(), rule.getRuleType());
            return false;
        }
        
        // Compare based on operator
        int comparison = valueToCompare.compareTo(threshold);
        switch (operator) {
            case ">=":
                return comparison >= 0;
            case "<=":
                return comparison <= 0;
            case ">":
                return comparison > 0;
            case "<":
                return comparison < 0;
            case "==":
                return comparison == 0;
            default:
                log.warn("Unknown operator: {}", operator);
                return true; // Skip unknown operators
        }
    }
}


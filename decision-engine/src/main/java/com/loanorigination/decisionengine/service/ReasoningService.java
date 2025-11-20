package com.loanorigination.decisionengine.service;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.DecisionReasoning;
import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.decisionengine.entity.RuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReasoningService {
    
    private static final Logger log = LoggerFactory.getLogger(ReasoningService.class);
    
    private final RuleConfigurationService ruleConfigurationService;
    
    public ReasoningService(RuleConfigurationService ruleConfigurationService) {
        this.ruleConfigurationService = ruleConfigurationService;
    }
    
    /**
     * Generates detailed reasoning for a loan decision
     */
    public DecisionReasoning generateReasoning(DecisionRequest request, 
                                               BigDecimal averageScore, 
                                               String finalDecision) {
        DecisionReasoning reasoning = new DecisionReasoning();
        
        // Set input values
        DecisionReasoning.DecisionInputs inputs = new DecisionReasoning.DecisionInputs();
        inputs.setLoanAmount(request.getLoanAmount());
        inputs.setBureauResponseCount(request.getBureauResponses().size());
        inputs.setApplicantAge(request.getApplicantAge());
        
        List<DecisionReasoning.DecisionInputs.BureauInput> bureauInputs = 
            request.getBureauResponses().stream()
                .map(br -> {
                    DecisionReasoning.DecisionInputs.BureauInput input = 
                        new DecisionReasoning.DecisionInputs.BureauInput();
                    input.setBureauName(br.getBureauName());
                    input.setCreditScore(br.getCreditScore());
                    input.setStatus(br.getStatus());
                    return input;
                })
                .collect(Collectors.toList());
        inputs.setBureauInputs(bureauInputs);
        reasoning.setInputs(inputs);
        
        // Set calculated values
        DecisionReasoning.CalculatedValues calculated = new DecisionReasoning.CalculatedValues();
        calculated.setAverageCreditScore(averageScore);
        
        List<BigDecimal> validScores = request.getBureauResponses().stream()
            .filter(r -> "SUCCESS".equals(r.getStatus()) && r.getCreditScore() != null)
            .map(BureauResponse::getCreditScore)
            .collect(Collectors.toList());
        calculated.setValidBureauCount(validScores.size());
        
        if (!validScores.isEmpty()) {
            BigDecimal minScore = validScores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxScore = validScores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            calculated.setCreditScoreRange(minScore + " - " + maxScore);
        }
        reasoning.setCalculated(calculated);
        
        // Evaluate rules dynamically from database
        evaluateDynamicRules(reasoning, request, averageScore);
        
        // Generate summary and decision path
        generateSummary(reasoning, finalDecision);
        generateDecisionPath(reasoning);
        
        return reasoning;
    }
    
    private void evaluateDynamicRules(DecisionReasoning reasoning, 
                                     DecisionRequest request, 
                                     BigDecimal averageScore) {
        List<RuleConfiguration> activeRules = ruleConfigurationService.getAllActiveRules();
        
        for (RuleConfiguration rule : activeRules) {
            BigDecimal valueToCompare = null;
            String actualValueStr;
            
            // Determine which value to compare based on rule type
            switch (rule.getRuleType()) {
                case "CREDIT_SCORE":
                    valueToCompare = averageScore;
                    actualValueStr = averageScore != null
                            ? averageScore.setScale(2, RoundingMode.HALF_UP).toString()
                            : "N/A";
                    break;
                case "LOAN_AMOUNT":
                    valueToCompare = request.getLoanAmount();
                    actualValueStr = request.getLoanAmount() != null
                            ? request.getLoanAmount().toString()
                            : "N/A";
                    break;
                case "BUREAU_RESPONSE":
                    long successCount = request.getBureauResponses().stream()
                        .filter(r -> "SUCCESS".equals(r.getStatus()))
                        .count();
                    valueToCompare = BigDecimal.valueOf(successCount);
                    actualValueStr = String.valueOf(successCount);
                    break;
                case "AGE_LIMIT":
                    valueToCompare = request.getApplicantAge();
                    actualValueStr = valueToCompare != null
                            ? valueToCompare.toPlainString()
                            : "N/A";
                    break;
                default:
                    log.warn("Unknown rule type: {}", rule.getRuleType());
                    continue; // Skip unknown rule types
            }
            
            if (valueToCompare == null) {
                String explanation = String.format("Required value for %s was not provided", rule.getRuleType());
                DecisionReasoning.RuleEvaluation evaluation = new DecisionReasoning.RuleEvaluation(
                    rule.getRuleName(),
                    rule.getDescription(),
                    false,
                    actualValueStr,
                    rule.getThresholdValue().toString(),
                    rule.getOperator(),
                    explanation,
                    rule.getImportance()
                );
                reasoning.addRuleEvaluation(evaluation);
                continue;
            }
            
            // Evaluate the rule
            boolean passed = evaluateRule(rule, valueToCompare);
            String explanation = generateExplanation(rule, valueToCompare, passed);
            
            DecisionReasoning.RuleEvaluation evaluation = new DecisionReasoning.RuleEvaluation(
                rule.getRuleName(),
                rule.getDescription(),
                passed,
                actualValueStr,
                rule.getThresholdValue().toString(),
                rule.getOperator(),
                explanation,
                rule.getImportance()
            );
            
            reasoning.addRuleEvaluation(evaluation);
        }
    }
    
    private boolean evaluateRule(RuleConfiguration rule, BigDecimal valueToCompare) {
        BigDecimal threshold = rule.getThresholdValue();
        String operator = rule.getOperator();
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
                return true;
        }
    }
    
    private String generateExplanation(RuleConfiguration rule, BigDecimal actualValue, boolean passed) {
        BigDecimal threshold = rule.getThresholdValue();
        String operator = rule.getOperator();
        
        if (passed) {
            return String.format("%s %.2f %s threshold %.2f - Rule passed", 
                rule.getRuleType(), actualValue, operator, threshold);
        } else {
            return String.format("%s %.2f does not meet requirement: %s %.2f - Rule failed", 
                rule.getRuleType(), actualValue, operator, threshold);
        }
    }
    
    private void generateSummary(DecisionReasoning reasoning, String finalDecision) {
        long passedRules = reasoning.getRuleEvaluations().stream()
            .filter(DecisionReasoning.RuleEvaluation::isPassed)
            .count();
        long totalRules = reasoning.getRuleEvaluations().size();
        
        String summary;
        if ("APPROVED".equals(finalDecision)) {
            summary = String.format(
                "Loan APPROVED: All %d critical rules passed. " +
                "Credit score of %.2f and loan amount of %s meet all requirements.",
                passedRules,
                reasoning.getCalculated().getAverageCreditScore(),
                reasoning.getInputs().getLoanAmount()
            );
        } else {
            List<String> failedRules = reasoning.getRuleEvaluations().stream()
                .filter(eval -> !eval.isPassed())
                .map(DecisionReasoning.RuleEvaluation::getRuleName)
                .collect(Collectors.toList());
            
            summary = String.format(
                "Loan REJECTED: %d out of %d rules failed. " +
                "Failed rules: %s. " +
                "Credit score: %.2f, Loan amount: %s",
                (totalRules - passedRules),
                totalRules,
                String.join(", ", failedRules),
                reasoning.getCalculated().getAverageCreditScore(),
                reasoning.getInputs().getLoanAmount()
            );
        }
        
        reasoning.setSummary(summary);
    }
    
    private void generateDecisionPath(DecisionReasoning reasoning) {
        List<String> pathSteps = new ArrayList<>();
        pathSteps.add("1. Received credit bureau responses");
        pathSteps.add("2. Calculated average credit score from valid responses");
        
        for (DecisionReasoning.RuleEvaluation eval : reasoning.getRuleEvaluations()) {
            String step = String.format(
                "%d. Evaluated %s: %s - %s",
                pathSteps.size() + 1,
                eval.getRuleName(),
                eval.isPassed() ? "PASSED" : "FAILED",
                eval.getExplanation()
            );
            pathSteps.add(step);
        }
        
        pathSteps.add(String.format(
            "%d. Final decision: %s",
            pathSteps.size() + 1,
            reasoning.getSummary()
        ));
        
        reasoning.setDecisionPath(String.join("\n", pathSteps));
    }
}


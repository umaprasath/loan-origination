package com.loanorigination.decisionengine.service;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.common.dto.DecisionResult;
import com.loanorigination.decisionengine.entity.RuleConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LLMDecisionService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LLMDecisionService.class);
    
    private final com.theokanning.openai.service.OpenAiService openAiService;
    private final OllamaClient ollamaClient;
    private final RuleConfigurationService ruleConfigurationService;
    private final boolean llmEnabled;
    private final String model;
    private final String provider; // "openai" or "ollama"
    
    public LLMDecisionService(
            @Value("${llm.enabled:false}") boolean llmEnabled,
            @Value("${llm.provider:openai}") String provider,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:gpt-4}") String model,
            RuleConfigurationService ruleConfigurationService,
            OllamaClient ollamaClient) {
        this.llmEnabled = llmEnabled;
        this.provider = provider.toLowerCase();
        this.model = model;
        this.ruleConfigurationService = ruleConfigurationService;
        this.ollamaClient = ollamaClient;
        
        if (!llmEnabled) {
            this.openAiService = null;
            log.info("LLM Decision Service is disabled");
        } else if ("ollama".equals(this.provider)) {
            this.openAiService = null;
            if (ollamaClient.isAvailable()) {
                log.info("LLM Decision Service initialized with Ollama, model: {}", model);
            } else {
                log.warn("Ollama is not available. LLM decision service will be disabled.");
            }
        } else if ("openai".equals(this.provider)) {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("LLM is enabled but OpenAI API key is not configured. LLM decision service will be disabled.");
                this.openAiService = null;
            } else {
                this.openAiService = new com.theokanning.openai.service.OpenAiService(apiKey);
                log.info("LLM Decision Service initialized with OpenAI, model: {}", model);
            }
        } else {
            this.openAiService = null;
            log.warn("Unknown LLM provider: {}. LLM decision service will be disabled.", provider);
        }
    }
    
    /**
     * Makes a loan decision using LLM with configured rules as context
     */
    public DecisionResult evaluateWithLLM(DecisionRequest request, BigDecimal averageCreditScore) {
        if (!llmEnabled) {
            throw new IllegalStateException("LLM service is not enabled");
        }
        
        if ("ollama".equals(provider) && !ollamaClient.isAvailable()) {
            throw new IllegalStateException("Ollama is not available");
        }
        
        if ("openai".equals(provider) && openAiService == null) {
            throw new IllegalStateException("OpenAI service is not configured");
        }
        
        log.info("Evaluating decision with LLM ({}) for request: {}", provider, request.getRequestId());
        
        try {
            // Build prompt with rules and application data
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildDecisionPrompt(request, averageCreditScore);
            
            String response;
            
            // Call appropriate LLM provider
            if ("ollama".equals(provider)) {
                response = ollamaClient.chat(model, systemPrompt, userPrompt);
            } else {
                // OpenAI
                com.theokanning.openai.completion.chat.ChatCompletionRequest chatRequest = com.theokanning.openai.completion.chat.ChatCompletionRequest.builder()
                        .model(model)
                        .messages(List.of(
                                new com.theokanning.openai.completion.chat.ChatMessage(com.theokanning.openai.completion.chat.ChatMessageRole.SYSTEM.value(), systemPrompt),
                                new com.theokanning.openai.completion.chat.ChatMessage(com.theokanning.openai.completion.chat.ChatMessageRole.USER.value(), userPrompt)
                        ))
                        .temperature(0.1) // Low temperature for consistent, rule-based decisions
                        .maxTokens(500)
                        .build();
                
                response = openAiService.createChatCompletion(chatRequest)
                        .getChoices()
                        .get(0)
                        .getMessage()
                        .getContent();
            }
            
            log.debug("LLM response: {}", response);
            
            // Parse LLM response
            DecisionResult result = parseLLMResponse(response, request.getRequestId(), averageCreditScore);
            
            log.info("LLM decision for request {}: {}", request.getRequestId(), result.getDecision());
            return result;
            
        } catch (Exception e) {
            log.error("Error calling LLM for request {}: {}", request.getRequestId(), e.getMessage(), e);
            throw new RuntimeException("LLM decision evaluation failed", e);
        }
    }
    
    private String buildSystemPrompt() {
        return "You are a loan decisioning expert. Analyze loan applications based on credit score, " +
               "income, debt, cashflow, and loan amount. Provide decisions in JSON format with " +
               "decision (APPROVED/REJECTED), creditScore, reason, and confidence (0-1). " +
               "Be conservative and follow the provided rules strictly.";
    }
    
    private String buildDecisionPrompt(DecisionRequest request, BigDecimal averageCreditScore) {
        StringBuilder prompt = new StringBuilder();
        
        // Add configured rules
        List<RuleConfiguration> rules = ruleConfigurationService.getAllActiveRules();
        prompt.append("## Decision Rules:\n");
        for (RuleConfiguration rule : rules) {
            prompt.append(String.format("- %s: %s (Threshold: %s %s, Importance: %s)\n",
                    rule.getRuleName(),
                    rule.getDescription(),
                    rule.getThresholdValue(),
                    rule.getOperator(),
                    rule.getImportance()));
        }
        prompt.append("\n");
        
        // Add application data
        prompt.append("## Loan Application Data:\n");
        prompt.append(String.format("- Average Credit Score: %.2f\n", averageCreditScore));
        prompt.append(String.format("- Loan Amount: %s\n", request.getLoanAmount()));
        if (request.getApplicantAge() != null) {
            prompt.append(String.format("- Applicant Age: %s\n", request.getApplicantAge()));
        }
        
        // Add financial data if available
        if (request.getAnnualIncome() != null) {
            prompt.append(String.format("- Annual Income: %s\n", request.getAnnualIncome()));
        }
        if (request.getTotalDebt() != null) {
            prompt.append(String.format("- Total Debt: %s\n", request.getTotalDebt()));
        }
        if (request.getMonthlyCashflow() != null) {
            prompt.append(String.format("- Monthly Cashflow: %s\n", request.getMonthlyCashflow()));
        }
        
        // Calculate debt-to-income ratio if available
        if (request.getAnnualIncome() != null && request.getTotalDebt() != null 
                && request.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dti = request.getTotalDebt()
                    .divide(request.getAnnualIncome(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            prompt.append(String.format("- Debt-to-Income Ratio: %.2f%%\n", dti));
        }
        
        // Add bureau responses
        prompt.append("- Credit Bureau Responses:\n");
        for (BureauResponse bureau : request.getBureauResponses()) {
            prompt.append(String.format("  - %s: Score=%s, Status=%s\n",
                    bureau.getBureauName(),
                    bureau.getCreditScore() != null ? bureau.getCreditScore() : "N/A",
                    bureau.getStatus()));
        }
        
        prompt.append("\n");
        prompt.append("## Analysis Required:\n");
        prompt.append("1. Evaluate each rule against the application data\n");
        prompt.append("2. Consider credit score, loan amount, and bureau responses\n");
        prompt.append("3. Provide a decision (APPROVED or REJECTED)\n");
        prompt.append("4. Explain your reasoning\n");
        prompt.append("5. Provide confidence score (0.0 to 1.0)\n");
        prompt.append("\n");
        prompt.append("Respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"decision\": \"APPROVED\" or \"REJECTED\",\n");
        prompt.append("  \"creditScore\": <number>,\n");
        prompt.append("  \"reason\": \"<explanation>\",\n");
        prompt.append("  \"confidence\": <0.0-1.0>\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private DecisionResult parseLLMResponse(String response, String requestId, BigDecimal averageCreditScore) {
        // Simple JSON parsing (in production, use proper JSON library)
        DecisionResult result = new DecisionResult();
        result.setRequestId(requestId);
        result.setCreditScore(averageCreditScore);
        result.setTimestamp(java.time.LocalDateTime.now());
        
        // Extract decision
        if (response.contains("\"decision\"") || response.contains("APPROVED")) {
            if (response.contains("\"APPROVED\"") || response.contains("APPROVED")) {
                result.setDecision("APPROVED");
            } else {
                result.setDecision("REJECTED");
            }
        } else {
            // Fallback parsing
            if (response.toLowerCase().contains("approve")) {
                result.setDecision("APPROVED");
            } else {
                result.setDecision("REJECTED");
            }
        }
        
        // Extract reason
        String reason = extractJsonField(response, "reason");
        if (reason == null || reason.isEmpty()) {
            reason = extractReasonFromText(response);
        }
        result.setReason(reason != null ? reason : "LLM decision based on configured rules");
        
        return result;
    }
    
    private String extractJsonField(String json, String fieldName) {
        try {
            int startIdx = json.indexOf("\"" + fieldName + "\"");
            if (startIdx == -1) return null;
            
            int colonIdx = json.indexOf(":", startIdx);
            int valueStart = json.indexOf("\"", colonIdx) + 1;
            int valueEnd = json.indexOf("\"", valueStart);
            
            if (valueStart > 0 && valueEnd > valueStart) {
                return json.substring(valueStart, valueEnd);
            }
        } catch (Exception e) {
            log.debug("Error extracting JSON field {}: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    private String extractReasonFromText(String text) {
        // Try to extract reasoning from text
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("reason") || line.toLowerCase().contains("because")) {
                return line.trim();
            }
        }
        return null;
    }
    
    public boolean isEnabled() {
        if (!llmEnabled) {
            return false;
        }
        
        if ("ollama".equals(provider)) {
            return ollamaClient.isAvailable();
        } else if ("openai".equals(provider)) {
            return openAiService != null;
        }
        
        return false;
    }
}


package com.loanorigination.decisionengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanorigination.common.dto.RuleConfigurationDTO;
import com.loanorigination.decisionengine.entity.Decision;
import com.loanorigination.decisionengine.repository.DecisionRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class RuleInferenceService {

    private static final Logger log = LoggerFactory.getLogger(RuleInferenceService.class);

    private final DecisionRepository decisionRepository;
    private final RuleConfigurationService ruleConfigurationService;
    private final boolean inferenceEnabled;
    private final String provider;
    private final String model;
    private final com.theokanning.openai.service.OpenAiService openAiService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public RuleInferenceService(DecisionRepository decisionRepository,
                                RuleConfigurationService ruleConfigurationService,
                                @Value("${llm.rules.enabled:${llm.enabled:false}}") boolean inferenceEnabled,
                                @Value("${llm.provider:openai}") String provider,
                                @Value("${llm.api-key:}") String apiKey,
                                @Value("${llm.model:gpt-4}") String model,
                                OllamaClient ollamaClient) {
        this.decisionRepository = decisionRepository;
        this.ruleConfigurationService = ruleConfigurationService;
        this.inferenceEnabled = inferenceEnabled;
        this.provider = provider.toLowerCase();
        this.model = model;
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();

        if (!inferenceEnabled) {
            this.openAiService = null;
            log.info("Rule inference is disabled");
        } else if ("openai".equals(this.provider)) {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("Rule inference enabled but OpenAI API key missing. Inference will be disabled.");
                this.openAiService = null;
            } else {
                this.openAiService = new com.theokanning.openai.service.OpenAiService(apiKey);
            }
        } else {
            this.openAiService = null;
        }
    }

    public RuleInferenceResult inferRules(int sampleSize) {
        if (!inferenceEnabled) {
            throw new IllegalStateException("Rule inference is disabled. Enable llm.rules.enabled or llm.enabled.");
        }

        List<Decision> decisions = decisionRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, Math.max(sampleSize, 1)));
        if (decisions.isEmpty()) {
            log.info("Skipping rule inference: no historical decisions available");
            RuleInferenceResult emptyResult = new RuleInferenceResult();
            emptyResult.setGeneratedRules(Collections.emptyList());
            emptyResult.setSkippedRules(Collections.emptyList());
            emptyResult.setMessage("No historical decisions available yet. Run credit checks to accumulate data before inferring rules.");
            return emptyResult;
        }

        String prompt = buildPrompt(decisions);
        String rawResponse = callModel(prompt);
        return parseAndPersist(rawResponse);
    }

    private String buildPrompt(List<Decision> decisions) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an expert credit risk analyst.\n");
        builder.append("Analyze the following historical loan decisions and propose decision rules.\n");
        builder.append("Identify clear threshold-based rules on credit score, loan amount, applicant age, income, debt, or bureau responses.\n");
        builder.append("Return JSON with structure {\"rules\": [{\"ruleName\":...,\"ruleType\":...,\"description\":...,\"operator\":...,\"thresholdValue\":...,\"importance\":...,\"confidence\":...,\"failureMessage\":...}]}\n");
        builder.append("ruleType must be one of CREDIT_SCORE, LOAN_AMOUNT, BUREAU_RESPONSE, AGE_LIMIT.\n");
        builder.append("Use uppercase snake_case ruleName. Threshold values must be numeric. Confidence is 0.0-1.0.\n\n");
        builder.append("### Historical Decisions (most recent first):\n");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (Decision decision : decisions) {
            builder.append(String.format("- decision: %s | creditScore: %s | loanAmount: %s | timestamp: %s | reason: %s\n",
                    decision.getDecision(),
                    safe(decision.getCreditScore()),
                    safe(decision.getLoanAmount()),
                    decision.getTimestamp() != null ? decision.getTimestamp().format(formatter) : "N/A",
                    decision.getReason() != null ? decision.getReason() : "N/A"));
        }
        builder.append("\nRespond with JSON only.\n");
        return builder.toString();
    }

    private String callModel(String prompt) {
        String systemPrompt = "You suggest credit decision rules based on historical data.";
        if ("ollama".equals(provider)) {
            if (!ollamaClient.isAvailable()) {
                throw new IllegalStateException("Ollama provider selected but not available.");
            }
            return ollamaClient.chat(model, systemPrompt, prompt);
        } else if ("openai".equals(provider) && openAiService != null) {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .temperature(0.2)
                    .maxTokens(800)
                    .build();
            ChatMessage response = openAiService.createChatCompletion(request).getChoices().get(0).getMessage();
            return response.getContent();
        }
        throw new IllegalStateException("LLM provider not configured for rule inference.");
    }

    private RuleInferenceResult parseAndPersist(String rawResponse) {
        RuleInferenceResult result = new RuleInferenceResult();
        result.setRawModelResponse(rawResponse);
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode rulesNode = root.path("rules");
            if (rulesNode.isMissingNode() || !rulesNode.isArray()) {
                log.warn("Model response missing rules array");
                result.setMessage("Model response missing rules array");
                return result;
            }
            List<RuleConfigurationDTO> persisted = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            Iterator<JsonNode> iterator = rulesNode.elements();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                try {
                    RuleConfigurationDTO dto = toRuleDto(node);
                    RuleConfigurationDTO saved = ruleConfigurationService.saveModelGeneratedRule(dto);
                    persisted.add(saved);
                } catch (Exception e) {
                    log.warn("Skipping rule due to parsing error: {}", e.getMessage());
                    skipped.add(node.toString());
                }
            }
            result.setGeneratedRules(persisted);
            result.setSkippedRules(skipped);
            result.setMessage(String.format("Persisted %d rules, skipped %d", persisted.size(), skipped.size()));
        } catch (Exception e) {
            log.error("Failed to parse model response: {}", rawResponse, e);
            result.setMessage("Failed to parse model response: " + e.getMessage());
        }
        return result;
    }

    private RuleConfigurationDTO toRuleDto(JsonNode node) {
        RuleConfigurationDTO dto = new RuleConfigurationDTO();
        dto.setRuleName(node.path("ruleName").asText());
        dto.setRuleType(node.path("ruleType").asText());
        dto.setDescription(node.path("description").asText());
        dto.setOperator(node.path("operator").asText());
        dto.setThresholdValue(new BigDecimal(node.path("thresholdValue").asText()));
        dto.setImportance(node.path("importance").asText(null));
        dto.setFailureMessage(node.path("failureMessage").asText(null));
        if (node.has("confidence")) {
            dto.setConfidenceScore(new BigDecimal(node.path("confidence").asText()));
        }
        dto.setSource("MODEL");
        dto.setUpdatedBy("MODEL_INFERENCE");
        dto.setMetadata(node.toString());
        return dto;
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "N/A";
    }

    public static class RuleInferenceResult {
        private List<RuleConfigurationDTO> generatedRules;
        private List<String> skippedRules;
        private String message;
        private String rawModelResponse;

        public List<RuleConfigurationDTO> getGeneratedRules() {
            return generatedRules;
        }

        public void setGeneratedRules(List<RuleConfigurationDTO> generatedRules) {
            this.generatedRules = generatedRules;
        }

        public List<String> getSkippedRules() {
            return skippedRules;
        }

        public void setSkippedRules(List<String> skippedRules) {
            this.skippedRules = skippedRules;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRawModelResponse() {
            return rawModelResponse;
        }

        public void setRawModelResponse(String rawModelResponse) {
            this.rawModelResponse = rawModelResponse;
        }
    }
}

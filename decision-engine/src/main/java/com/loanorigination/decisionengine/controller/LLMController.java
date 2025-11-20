package com.loanorigination.decisionengine.controller;

import com.loanorigination.decisionengine.service.LLMDecisionService;
import com.loanorigination.decisionengine.service.OllamaClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@Tag(name = "LLM Management", description = "API for managing and monitoring LLM (Large Language Model) integration for loan decisioning")
public class LLMController {
    
    private final LLMDecisionService llmDecisionService;
    private final OllamaClient ollamaClient;
    private final String provider;
    private final String model;
    private final boolean enabled;
    private final WebClient webClient;
    
    public LLMController(
            LLMDecisionService llmDecisionService,
            OllamaClient ollamaClient,
            @Value("${llm.provider:openai}") String provider,
            @Value("${llm.model:gpt-4}") String model,
            @Value("${llm.enabled:false}") boolean enabled,
            @Value("${llm.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.llmDecisionService = llmDecisionService;
        this.ollamaClient = ollamaClient;
        this.provider = provider;
        this.model = model;
        this.enabled = enabled;
        this.webClient = WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
    }
    
    @Operation(
            summary = "Get LLM status and configuration",
            description = "Returns the current LLM configuration including provider, model, enabled status, and availability"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "LLM status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("provider", provider);
        status.put("model", model);
        status.put("available", llmDecisionService.isEnabled());
        
        if ("ollama".equals(provider)) {
            status.put("ollamaAvailable", ollamaClient.isAvailable());
        }
        
        return ResponseEntity.ok(status);
    }
    
    @Operation(
            summary = "Test LLM connection",
            description = "Tests the connection to the configured LLM provider (OpenAI or Ollama) and returns connection status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Connection test completed",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        if (!enabled) {
            result.put("success", false);
            result.put("message", "LLM is not enabled");
            return ResponseEntity.ok(result);
        }
        
        boolean available = llmDecisionService.isEnabled();
        result.put("success", available);
        result.put("provider", provider);
        result.put("model", model);
        
        if (available) {
            result.put("message", "LLM connection successful");
        } else {
            result.put("message", "LLM connection failed. Check configuration and service availability.");
        }
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "Get available Ollama models",
            description = "Retrieves a list of available models from the Ollama server. Only works when provider is 'ollama'"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Models retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ollama is not the configured provider"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Ollama server is not available"
            )
    })
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        Map<String, Object> result = new HashMap<>();
        
        if (!"ollama".equals(provider)) {
            result.put("error", "Ollama is not the configured provider. Current provider: " + provider);
            return ResponseEntity.badRequest().body(result);
        }
        
        if (!ollamaClient.isAvailable()) {
            result.put("error", "Ollama server is not available");
            return ResponseEntity.status(503).body(result);
        }
        
        try {
            String response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            result.put("success", true);
            result.put("models", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to retrieve models: " + e.getMessage());
            return ResponseEntity.status(503).body(result);
        }
    }
    
    @Operation(
            summary = "Get LLM configuration",
            description = "Returns the current LLM configuration settings (without sensitive information like API keys)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", enabled);
        config.put("provider", provider);
        config.put("model", model);
        
        if ("ollama".equals(provider)) {
            // Get Ollama base URL from the client (if accessible)
            config.put("ollamaBaseUrl", "configured");
        }
        
        return ResponseEntity.ok(config);
    }
    
    @Operation(
            summary = "Health check for LLM service",
            description = "Performs a health check on the LLM service and returns detailed health information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Health check completed",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("enabled", enabled);
        health.put("provider", provider);
        health.put("model", model);
        health.put("status", llmDecisionService.isEnabled() ? "UP" : "DOWN");
        
        if ("ollama".equals(provider)) {
            health.put("ollamaAvailable", ollamaClient.isAvailable());
        }
        
        return ResponseEntity.ok(health);
    }
}


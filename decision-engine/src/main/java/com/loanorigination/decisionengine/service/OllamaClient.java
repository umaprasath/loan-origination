package com.loanorigination.decisionengine.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class OllamaClient {
    
    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    
    private final WebClient webClient;
    
    public OllamaClient(@org.springframework.beans.factory.annotation.Value("${llm.ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("Ollama client initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Calls Ollama API for chat completion
     */
    public String chat(String model, String systemPrompt, String userPrompt) {
        try {
            ChatRequest request = new ChatRequest();
            request.setModel(model);
            request.setMessages(List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userPrompt)
            ));
            request.setStream(false);
            request.setOptions(java.util.Map.of("temperature", 0.1)); // Low temperature for consistent decisions
            
            ChatResponse response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();
            
            if (response != null && response.getMessage() != null) {
                return response.getMessage().getContent();
            }
            
            throw new RuntimeException("Empty response from Ollama");
            
        } catch (Exception e) {
            log.error("Error calling Ollama API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Ollama: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if Ollama is available
     */
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatRequest {
        private String model;
        private List<Message> messages;
        private boolean stream;
        private Map<String, Object> options;
        
        // Explicit getters and setters
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public List<Message> getMessages() {
            return messages;
        }
        
        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
        
        public boolean isStream() {
            return stream;
        }
        
        public void setStream(boolean stream) {
            this.stream = stream;
        }
        
        public Map<String, Object> getOptions() {
            return options;
        }
        
        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        private String role;
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public Message() {
        }
        
        // Explicit getters and setters
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatResponse {
        private Message message;
        @JsonProperty("done")
        private boolean done;
        
        // Explicit getters and setters
        public Message getMessage() {
            return message;
        }
        
        public void setMessage(Message message) {
            this.message = message;
        }
        
        public boolean isDone() {
            return done;
        }
        
        public void setDone(boolean done) {
            this.done = done;
        }
    }
}


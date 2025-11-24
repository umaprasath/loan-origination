package com.loanorigination.equifax.service;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.event.CreditBureauEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EquifaxService {
    
    private static final Logger log = LoggerFactory.getLogger(EquifaxService.class);
    
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, CreditBureauEvent> kafkaTemplate;
    private Map<String, BigDecimal> configuredCreditScores;
    
    public EquifaxService(
            WebClient.Builder webClientBuilder, 
            KafkaTemplate<String, CreditBureauEvent> kafkaTemplate) {
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
        this.configuredCreditScores = new HashMap<>();
        // Load configured scores from application.yml
        // You can add SSNs in application.yml under mock.credit-scores.equifax
    }
    
    @Value("#{${mock.credit-scores.equifax:{}}}")
    public void setConfiguredCreditScores(Map<String, Integer> scores) {
        if (scores != null) {
            scores.forEach((ssn, score) -> 
                configuredCreditScores.put(ssn.replaceAll("[^0-9]", ""), new BigDecimal(score)));
        }
    }
    
    // Mock Equifax API URL - in production, this would be the actual Equifax API endpoint
    private static final String EQUIFAX_API_URL = "https://api.equifax.com/v1/credit";
    private static final String KAFKA_TOPIC = "credit-bureau-events";
    
    public BureauResponse checkCredit(CreditRequest request) {
        log.info("Calling Equifax API for SSN: {}", maskSsn(request.getSsn()));
        
        BureauResponse response = new BureauResponse(
                "EQUIFAX",
                null, // creditScore - will be set later
                null, // status - will be set later
                null, // errorMessage
                LocalDateTime.now()
        );
        
        try {
            // In production, this would make an actual REST/SOAP call to Equifax API over TLS
            // For now, simulating the API call
            // WebClient webClient = webClientBuilder.baseUrl(EQUIFAX_API_URL).build();
            
            // Simulate API call - in real implementation, this would be:
            // webClient.post()
            //     .uri("/check")
            //     .header("Authorization", "Bearer {token}")
            //     .bodyValue(request)
            //     .retrieve()
            //     .bodyToMono(EquifaxApiResponse.class)
            
            // Mock response for demonstration
            BigDecimal creditScore = generateMockCreditScore(request.getSsn());
            response = new BureauResponse(
                    "EQUIFAX",
                    creditScore,
                    "SUCCESS",
                    null,
                    LocalDateTime.now()
            );
            
            log.info("Equifax API call successful. Credit score: {}", creditScore);
            
        } catch (Exception e) {
            log.error("Error calling Equifax API: {}", e.getMessage());
            response = new BureauResponse(
                    "EQUIFAX",
                    null,
                    "FAILED",
                    "Equifax API unavailable: " + e.getMessage(),
                    LocalDateTime.now()
            );
        }
        
        // Publish event to Kafka
        publishEvent(request, response);
        
        return response;
    }
    
    private BigDecimal generateMockCreditScore(String ssn) {
        // Normalize SSN (remove dashes and spaces)
        String normalizedSsn = ssn.replaceAll("[^0-9]", "");
        
        // Check if there's a configured credit score for this SSN
        if (configuredCreditScores.containsKey(normalizedSsn)) {
            log.debug("Using configured credit score for SSN: {}", maskSsn(ssn));
            return configuredCreditScores.get(normalizedSsn);
        }
        
        // Fall back to hash-based generation
        // Mock credit score generation based on SSN hash
        // In production, this would come from the actual Equifax API
        int hash = ssn.hashCode();
        int score = 500 + Math.abs((hash * 7) % 400); // Different calculation for variety
        return new BigDecimal(score);
    }
    
    private void publishEvent(CreditRequest request, BureauResponse response) {
        try {
            CreditBureauEvent event = new CreditBureauEvent(
                    UUID.randomUUID().toString(),
                    request.getSsn(), // Using SSN as request identifier for demo
                    "EQUIFAX",
                    response,
                    LocalDateTime.now()
            );
            
            kafkaTemplate.send(KAFKA_TOPIC, event.getEventId(), event);
            log.debug("Published Equifax event to Kafka: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error publishing to Kafka: {}", e.getMessage());
        }
    }
    
    private String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 4) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
}


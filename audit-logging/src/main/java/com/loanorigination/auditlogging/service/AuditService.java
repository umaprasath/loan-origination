package com.loanorigination.auditlogging.service;

import com.loanorigination.auditlogging.entity.AuditLogEntity;
import com.loanorigination.auditlogging.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {
    
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public AuditService(AuditLogRepository auditLogRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    private static final String AUDIT_TOPIC = "audit-events";
    
    @Transactional
    public void logEvent(String requestId, String serviceName, String action, Object details) {
        log.info("Logging audit event for request: {}, service: {}, action: {}", requestId, serviceName, action);
        
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setRequestId(requestId);
        auditLog.setServiceName(serviceName);
        auditLog.setAction(action);
        auditLog.setDetails(details != null ? details.toString() : null);
        auditLog.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(auditLog);
        
        // Publish to Kafka for async processing
        try {
            kafkaTemplate.send(AUDIT_TOPIC, auditLog.getId(), auditLog);
            log.debug("Published audit event to Kafka: {}", auditLog.getId());
        } catch (Exception e) {
            log.error("Error publishing to Kafka: {}", e.getMessage());
        }
    }
}


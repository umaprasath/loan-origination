package com.loanorigination.auditlogging.controller;

import com.loanorigination.auditlogging.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    
    private final AuditService auditService;
    
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/log")
    public ResponseEntity<Void> logEvent(@RequestBody Map<String, Object> logRequest) {
        String requestId = (String) logRequest.get("requestId");
        String serviceName = (String) logRequest.get("serviceName");
        String action = (String) logRequest.get("action");
        Object details = logRequest.get("details");
        
        auditService.logEvent(requestId, serviceName, action, details);
        return ResponseEntity.ok().build();
    }
}


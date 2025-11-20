package com.loanorigination.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private String id;
    private String requestId;
    private String serviceName;
    private String action;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    private String userId;
}


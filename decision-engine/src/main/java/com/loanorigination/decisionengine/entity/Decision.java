package com.loanorigination.decisionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Decision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String requestId;
    
    @Column(nullable = false)
    private String decision;
    
    @Column(nullable = false)
    private BigDecimal creditScore;
    
    @Column(nullable = false)
    private BigDecimal loanAmount;
    
    private String reason;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // Explicit getters for Lombok compatibility
    public String getRequestId() {
        return requestId;
    }
    
    public String getDecision() {
        return decision;
    }
    
    public BigDecimal getCreditScore() {
        return creditScore;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    // Explicit setters for Lombok compatibility
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public void setDecision(String decision) {
        this.decision = decision;
    }
    
    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}


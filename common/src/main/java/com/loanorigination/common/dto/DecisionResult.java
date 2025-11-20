package com.loanorigination.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Decision result from the decision engine")
public class DecisionResult {
    @Schema(description = "Unique request identifier", example = "a615ad19-9ab5-482f-b0ae-7d323d7287d4")
    private String requestId;
    
    @Schema(description = "Loan decision", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED"})
    private String decision; // APPROVED, REJECTED
    
    @Schema(description = "Calculated credit score", example = "699.00")
    private BigDecimal creditScore;
    
    @Schema(description = "Reason for the decision", example = "Credit score and loan amount meet requirements")
    private String reason;
    
    @Schema(description = "Timestamp of the decision", example = "2025-11-06T12:18:06.472785")
    private LocalDateTime timestamp;
    
    @Schema(description = "Detailed reasoning explaining how the decision was made")
    private DecisionReasoning reasoning;
    
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
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public DecisionReasoning getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(DecisionReasoning reasoning) {
        this.reasoning = reasoning;
    }
}


package com.loanorigination.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "Credit check response containing loan decision and credit bureau information")
public class CreditResponse {
    @Schema(description = "Unique request identifier", example = "a615ad19-9ab5-482f-b0ae-7d323d7287d4")
    private String requestId;
    
    @Schema(description = "Loan decision status", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED", "PENDING"})
    private String status; // APPROVED, REJECTED, PENDING
    
    @Schema(description = "Average credit score from all bureaus", example = "699.00")
    private BigDecimal creditScore;
    
    @Schema(description = "Requested loan amount", example = "50000")
    private BigDecimal loanAmount;
    
    @Schema(description = "Reason for the decision", example = "Credit score and loan amount meet requirements")
    private String decisionReason;
    
    @Schema(description = "Timestamp of the decision", example = "2025-11-06T12:18:06.472785")
    private LocalDateTime timestamp;
    
    // Bureau responses
    @Schema(description = "Credit response from Experian bureau")
    private BureauResponse experianResponse;
    
    @Schema(description = "Credit response from Equifax bureau")
    private BureauResponse equifaxResponse;
    
    @Schema(description = "Detailed decision reasoning explaining how the loan decision was made")
    private DecisionReasoning reasoning;
    
    // Explicit constructor for Lombok compatibility
    @JsonCreator
    public CreditResponse(@JsonProperty("requestId") String requestId, 
                         @JsonProperty("status") String status, 
                         @JsonProperty("creditScore") BigDecimal creditScore, 
                         @JsonProperty("loanAmount") BigDecimal loanAmount,
                         @JsonProperty("decisionReason") String decisionReason, 
                         @JsonProperty("timestamp") LocalDateTime timestamp, 
                         @JsonProperty("experianResponse") BureauResponse experianResponse,
                         @JsonProperty("equifaxResponse") BureauResponse equifaxResponse) {
        this.requestId = requestId;
        this.status = status;
        this.creditScore = creditScore;
        this.loanAmount = loanAmount;
        this.decisionReason = decisionReason;
        this.timestamp = timestamp;
        this.experianResponse = experianResponse;
        this.equifaxResponse = equifaxResponse;
    }
    
    // Explicit getters and setters for Jackson compatibility
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getCreditScore() {
        return creditScore;
    }
    
    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public String getDecisionReason() {
        return decisionReason;
    }
    
    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BureauResponse getExperianResponse() {
        return experianResponse;
    }
    
    public void setExperianResponse(BureauResponse experianResponse) {
        this.experianResponse = experianResponse;
    }
    
    public BureauResponse getEquifaxResponse() {
        return equifaxResponse;
    }
    
    public void setEquifaxResponse(BureauResponse equifaxResponse) {
        this.equifaxResponse = equifaxResponse;
    }
    
    public DecisionReasoning getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(DecisionReasoning reasoning) {
        this.reasoning = reasoning;
    }
}


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
@Schema(description = "Credit bureau response containing credit score and status")
public class BureauResponse {
    @Schema(description = "Name of the credit bureau", example = "EXPERIAN", allowableValues = {"EXPERIAN", "EQUIFAX"})
    private String bureauName; // EXPERIAN, EQUIFAX
    
    @Schema(description = "Credit score from the bureau", example = "750")
    private BigDecimal creditScore;
    
    @Schema(description = "Status of the bureau response", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "TIMEOUT"})
    private String status; // SUCCESS, FAILED, TIMEOUT
    
    @Schema(description = "Error message if the request failed", example = "Service unavailable")
    private String errorMessage;
    
    @Schema(description = "Timestamp of the bureau response", example = "2025-11-06T12:18:06.461779")
    private LocalDateTime timestamp;
    
    // Explicit constructor for Lombok compatibility
    @JsonCreator
    public BureauResponse(@JsonProperty("bureauName") String bureauName, 
                         @JsonProperty("creditScore") BigDecimal creditScore, 
                         @JsonProperty("status") String status, 
                         @JsonProperty("errorMessage") String errorMessage, 
                         @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.bureauName = bureauName;
        this.creditScore = creditScore;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }
    
    // Explicit getters for Lombok compatibility
    public String getBureauName() {
        return bureauName;
    }
    
    public BigDecimal getCreditScore() {
        return creditScore;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    // Explicit setters for Lombok compatibility
    public void setBureauName(String bureauName) {
        this.bureauName = bureauName;
    }
    
    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}


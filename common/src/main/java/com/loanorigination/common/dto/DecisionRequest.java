package com.loanorigination.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class DecisionRequest {
    private String requestId;
    private BigDecimal loanAmount;
    private List<BureauResponse> bureauResponses;
    
    // Extended financial data for LLM evaluation
    private BigDecimal annualIncome;
    private BigDecimal totalDebt;
    private BigDecimal monthlyCashflow;
    private BigDecimal applicantAge;

    public BigDecimal getApplicantAge() {
        return applicantAge;
    }

    public void setApplicantAge(BigDecimal applicantAge) {
        this.applicantAge = applicantAge;
    }
    
    // Explicit constructor for Lombok compatibility
    @JsonCreator
    public DecisionRequest(@JsonProperty("requestId") String requestId, 
                          @JsonProperty("loanAmount") BigDecimal loanAmount, 
                          @JsonProperty("bureauResponses") List<BureauResponse> bureauResponses) {
        this.requestId = requestId;
        this.loanAmount = loanAmount;
        this.bureauResponses = bureauResponses;
    }
    
    // Explicit getters for Lombok compatibility
    public String getRequestId() {
        return requestId;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public List<BureauResponse> getBureauResponses() {
        return bureauResponses;
    }
    
    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }
    
    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }
    
    public BigDecimal getTotalDebt() {
        return totalDebt;
    }
    
    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }
    
    public BigDecimal getMonthlyCashflow() {
        return monthlyCashflow;
    }
    
    public void setMonthlyCashflow(BigDecimal monthlyCashflow) {
        this.monthlyCashflow = monthlyCashflow;
    }
}


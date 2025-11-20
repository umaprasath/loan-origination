package com.loanorigination.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credit check request containing applicant and loan information")
public class CreditRequest {
    @NotBlank(message = "SSN is required")
    @Schema(description = "Social Security Number", example = "123-45-6789", required = true)
    private String ssn;
    
    @NotBlank(message = "First name is required")
    @Schema(description = "Applicant's first name", example = "John", required = true)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Schema(description = "Applicant's last name", example = "Doe", required = true)
    private String lastName;
    
    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be positive")
    @Schema(description = "Requested loan amount", example = "50000", required = true)
    private BigDecimal loanAmount;
    
    @Schema(description = "Purpose of the loan", example = "Home Purchase")
    private String loanPurpose;
    
    @Schema(description = "Applicant's annual income", example = "75000")
    private BigDecimal annualIncome;
    
    @Schema(description = "Total outstanding debt", example = "15000")
    private BigDecimal totalDebt;
    
    @Schema(description = "Monthly cashflow (income - expenses)", example = "3500")
    private BigDecimal monthlyCashflow;
    
    @Schema(description = "Applicant age in years", example = "35")
    private Integer applicantAge;
    
    // Explicit getters for Lombok compatibility
    public String getSsn() {
        return ssn;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public String getLoanPurpose() {
        return loanPurpose;
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
    
    public Integer getApplicantAge() {
        return applicantAge;
    }
    
    public void setApplicantAge(Integer applicantAge) {
        this.applicantAge = applicantAge;
    }
}


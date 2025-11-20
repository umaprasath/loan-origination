package com.loanorigination.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Detailed reasoning explaining how a loan decision was made")
public class DecisionReasoning {
    
    @Schema(description = "Overall decision explanation", example = "Loan approved based on credit score and amount validation")
    private String summary;
    
    @Schema(description = "List of rules evaluated during decision making")
    private List<RuleEvaluation> ruleEvaluations;
    
    @Schema(description = "Input values used in the decision")
    private DecisionInputs inputs;
    
    @Schema(description = "Calculated values used in the decision")
    private CalculatedValues calculated;
    
    @Schema(description = "Decision path showing the flow of rule evaluations")
    private String decisionPath;
    
    public DecisionReasoning() {
        this.ruleEvaluations = new ArrayList<>();
    }
    
    public DecisionReasoning(String summary) {
        this.summary = summary;
        this.ruleEvaluations = new ArrayList<>();
    }
    
    public void addRuleEvaluation(RuleEvaluation evaluation) {
        if (this.ruleEvaluations == null) {
            this.ruleEvaluations = new ArrayList<>();
        }
        this.ruleEvaluations.add(evaluation);
    }
    
    // Explicit getters and setters
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<RuleEvaluation> getRuleEvaluations() {
        return ruleEvaluations;
    }
    
    public void setRuleEvaluations(List<RuleEvaluation> ruleEvaluations) {
        this.ruleEvaluations = ruleEvaluations;
    }
    
    public DecisionInputs getInputs() {
        return inputs;
    }
    
    public void setInputs(DecisionInputs inputs) {
        this.inputs = inputs;
    }
    
    public CalculatedValues getCalculated() {
        return calculated;
    }
    
    public void setCalculated(CalculatedValues calculated) {
        this.calculated = calculated;
    }
    
    public String getDecisionPath() {
        return decisionPath;
    }
    
    public void setDecisionPath(String decisionPath) {
        this.decisionPath = decisionPath;
    }
    
    @Data
    @Schema(description = "Evaluation result of a single decision rule")
    public static class RuleEvaluation {
        
        public RuleEvaluation() {
            // No-arg constructor for Jackson
        }
        @Schema(description = "Name of the rule", example = "MINIMUM_CREDIT_SCORE")
        private String ruleName;
        
        @Schema(description = "Human-readable description of the rule", example = "Credit score must be at least 650")
        private String ruleDescription;
        
        @Schema(description = "Whether the rule passed", example = "true")
        private boolean passed;
        
        @Schema(description = "Actual value used in evaluation", example = "699.00")
        private String actualValue;
        
        @Schema(description = "Threshold or expected value", example = "650.00")
        private String threshold;
        
        @Schema(description = "Comparison operator used", example = ">=")
        private String operator;
        
        @Schema(description = "Detailed explanation of the rule evaluation", example = "Credit score 699.00 is greater than or equal to minimum threshold 650.00")
        private String explanation;
        
        @Schema(description = "Weight or importance of this rule in the decision", example = "HIGH")
        private String importance;
        
        public RuleEvaluation(String ruleName, String ruleDescription, boolean passed, 
                            String actualValue, String threshold, String operator, 
                            String explanation, String importance) {
            this.ruleName = ruleName;
            this.ruleDescription = ruleDescription;
            this.passed = passed;
            this.actualValue = actualValue;
            this.threshold = threshold;
            this.operator = operator;
            this.explanation = explanation;
            this.importance = importance;
        }
        
        // Explicit getters and setters
        public String getRuleName() {
            return ruleName;
        }
        
        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }
        
        public String getRuleDescription() {
            return ruleDescription;
        }
        
        public void setRuleDescription(String ruleDescription) {
            this.ruleDescription = ruleDescription;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public String getActualValue() {
            return actualValue;
        }
        
        public void setActualValue(String actualValue) {
            this.actualValue = actualValue;
        }
        
        public String getThreshold() {
            return threshold;
        }
        
        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
        
        public String getOperator() {
            return operator;
        }
        
        public void setOperator(String operator) {
            this.operator = operator;
        }
        
        public String getExplanation() {
            return explanation;
        }
        
        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
        
        public String getImportance() {
            return importance;
        }
        
        public void setImportance(String importance) {
            this.importance = importance;
        }
    }
    
    @Data
    @Schema(description = "Input values used in the decision")
    public static class DecisionInputs {
        
        public DecisionInputs() {
            // No-arg constructor for Jackson
        }
        @Schema(description = "Requested loan amount", example = "50000")
        private BigDecimal loanAmount;
        
        @Schema(description = "Number of bureau responses received", example = "2")
        private Integer bureauResponseCount;
        
        @Schema(description = "Applicant age in years", example = "35")
        private BigDecimal applicantAge;
        
        @Schema(description = "List of bureau responses")
        private List<BureauInput> bureauInputs;
        
        @Data
        @Schema(description = "Input from a credit bureau")
        public static class BureauInput {
            
            public BureauInput() {
                // No-arg constructor for Jackson
            }
            @Schema(description = "Bureau name", example = "EXPERIAN")
            private String bureauName;
            
            @Schema(description = "Credit score from bureau", example = "750")
            private BigDecimal creditScore;
            
            @Schema(description = "Status of bureau response", example = "SUCCESS")
            private String status;
            
            // Explicit getters and setters
            public String getBureauName() {
                return bureauName;
            }
            
            public void setBureauName(String bureauName) {
                this.bureauName = bureauName;
            }
            
            public BigDecimal getCreditScore() {
                return creditScore;
            }
            
            public void setCreditScore(BigDecimal creditScore) {
                this.creditScore = creditScore;
            }
            
            public String getStatus() {
                return status;
            }
            
            public void setStatus(String status) {
                this.status = status;
            }
        }
        
        // Explicit getters and setters
        public BigDecimal getLoanAmount() {
            return loanAmount;
        }
        
        public void setLoanAmount(BigDecimal loanAmount) {
            this.loanAmount = loanAmount;
        }
        
        public Integer getBureauResponseCount() {
            return bureauResponseCount;
        }
        
        public void setBureauResponseCount(Integer bureauResponseCount) {
            this.bureauResponseCount = bureauResponseCount;
        }
        
        public BigDecimal getApplicantAge() {
            return applicantAge;
        }
        
        public void setApplicantAge(BigDecimal applicantAge) {
            this.applicantAge = applicantAge;
        }
        
        public List<BureauInput> getBureauInputs() {
            return bureauInputs;
        }
        
        public void setBureauInputs(List<BureauInput> bureauInputs) {
            this.bureauInputs = bureauInputs;
        }
    }
    
    @Data
    @Schema(description = "Calculated values used in the decision")
    public static class CalculatedValues {
        
        public CalculatedValues() {
            // No-arg constructor for Jackson
        }
        @Schema(description = "Average credit score calculated from all bureaus", example = "699.00")
        private BigDecimal averageCreditScore;
        
        @Schema(description = "Number of valid bureau responses used in calculation", example = "2")
        private Integer validBureauCount;
        
        @Schema(description = "Credit score range", example = "583 - 815")
        private String creditScoreRange;
        
        // Explicit getters and setters
        public BigDecimal getAverageCreditScore() {
            return averageCreditScore;
        }
        
        public void setAverageCreditScore(BigDecimal averageCreditScore) {
            this.averageCreditScore = averageCreditScore;
        }
        
        public Integer getValidBureauCount() {
            return validBureauCount;
        }
        
        public void setValidBureauCount(Integer validBureauCount) {
            this.validBureauCount = validBureauCount;
        }
        
        public String getCreditScoreRange() {
            return creditScoreRange;
        }
        
        public void setCreditScoreRange(String creditScoreRange) {
            this.creditScoreRange = creditScoreRange;
        }
    }
}


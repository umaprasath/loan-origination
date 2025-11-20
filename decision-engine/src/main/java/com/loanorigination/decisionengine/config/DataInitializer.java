package com.loanorigination.decisionengine.config;

import com.loanorigination.decisionengine.entity.RuleConfiguration;
import com.loanorigination.decisionengine.repository.RuleConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final RuleConfigurationRepository repository;
    
    public DataInitializer(RuleConfigurationRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public void run(String... args) {
        // Initialize default rules if they don't exist
        initializeDefaultRules();
    }
    
    private void initializeDefaultRules() {
        // MINIMUM_CREDIT_SCORE rule
        if (repository.findByRuleName("MINIMUM_CREDIT_SCORE").isEmpty()) {
            RuleConfiguration rule1 = new RuleConfiguration();
            rule1.setRuleName("MINIMUM_CREDIT_SCORE");
            rule1.setRuleType("CREDIT_SCORE");
            rule1.setDescription("Credit score must be at least 650");
            rule1.setThresholdValue(new BigDecimal("650"));
            rule1.setOperator(">=");
            rule1.setEnabled(true);
            rule1.setPriority(1);
            rule1.setImportance("CRITICAL");
            rule1.setFailureMessage("Credit score below minimum threshold");
            repository.save(rule1);
            log.info("Initialized default rule: MINIMUM_CREDIT_SCORE");
        }
        
        // MAXIMUM_LOAN_AMOUNT rule
        if (repository.findByRuleName("MAXIMUM_LOAN_AMOUNT").isEmpty()) {
            RuleConfiguration rule2 = new RuleConfiguration();
            rule2.setRuleName("MAXIMUM_LOAN_AMOUNT");
            rule2.setRuleType("LOAN_AMOUNT");
            rule2.setDescription("Loan amount must not exceed 1,000,000");
            rule2.setThresholdValue(new BigDecimal("1000000"));
            rule2.setOperator("<=");
            rule2.setEnabled(true);
            rule2.setPriority(2);
            rule2.setImportance("CRITICAL");
            rule2.setFailureMessage("Loan amount exceeds maximum limit");
            repository.save(rule2);
            log.info("Initialized default rule: MAXIMUM_LOAN_AMOUNT");
        }
        
        // BUREAU_RESPONSE_VALIDATION rule
        if (repository.findByRuleName("BUREAU_RESPONSE_VALIDATION").isEmpty()) {
            RuleConfiguration rule3 = new RuleConfiguration();
            rule3.setRuleName("BUREAU_RESPONSE_VALIDATION");
            rule3.setRuleType("BUREAU_RESPONSE");
            rule3.setDescription("At least one credit bureau must respond successfully");
            rule3.setThresholdValue(new BigDecimal("1"));
            rule3.setOperator(">=");
            rule3.setEnabled(true);
            rule3.setPriority(3);
            rule3.setImportance("HIGH");
            rule3.setFailureMessage("No successful bureau responses received");
            repository.save(rule3);
            log.info("Initialized default rule: BUREAU_RESPONSE_VALIDATION");
        }
    }
}


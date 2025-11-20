package com.loanorigination.decisionengine.repository;

import com.loanorigination.decisionengine.entity.RuleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleConfigurationRepository extends JpaRepository<RuleConfiguration, Long> {
    
    Optional<RuleConfiguration> findByRuleName(String ruleName);
    
    List<RuleConfiguration> findByEnabledTrueOrderByPriorityAsc();
    
    List<RuleConfiguration> findByRuleType(String ruleType);
    
    @Query("SELECT r FROM RuleConfiguration r WHERE r.enabled = true ORDER BY r.priority ASC")
    List<RuleConfiguration> findAllActiveRules();
}


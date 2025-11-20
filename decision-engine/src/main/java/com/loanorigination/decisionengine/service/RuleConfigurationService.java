package com.loanorigination.decisionengine.service;

import com.loanorigination.common.dto.RuleConfigurationDTO;
import com.loanorigination.decisionengine.entity.RuleConfiguration;
import com.loanorigination.decisionengine.repository.RuleConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RuleConfigurationService {
    
    private static final Logger log = LoggerFactory.getLogger(RuleConfigurationService.class);
    private static final String CACHE_NAME = "ruleConfigurations";
    
    private final RuleConfigurationRepository repository;
    
    public RuleConfigurationService(RuleConfigurationRepository repository) {
        this.repository = repository;
    }
    
    @Cacheable(value = CACHE_NAME, key = "'all-active'")
    public List<RuleConfiguration> getAllActiveRules() {
        log.debug("Fetching all active rules from database");
        return repository.findAllActiveRules();
    }
    
    @Cacheable(value = CACHE_NAME, key = "#ruleName")
    public RuleConfiguration getRuleByName(String ruleName) {
        return repository.findByRuleName(ruleName)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleName));
    }
    
    public List<RuleConfigurationDTO> getAllRules() {
        return repository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    public RuleConfigurationDTO getRuleById(Long id) {
        RuleConfiguration rule = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found with id: " + id));
        return toDTO(rule);
    }
    
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RuleConfigurationDTO createRule(RuleConfigurationDTO dto) {
        log.info("Creating new rule: {}", dto.getRuleName());
        
        // Check if rule name already exists
        if (repository.findByRuleName(dto.getRuleName()).isPresent()) {
            throw new IllegalArgumentException("Rule with name " + dto.getRuleName() + " already exists");
        }
        
        RuleConfiguration rule = toEntity(dto);
        RuleConfiguration saved = repository.save(rule);
        log.info("Rule created successfully: {}", saved.getRuleName());
        
        return toDTO(saved);
    }
    
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RuleConfigurationDTO updateRule(Long id, RuleConfigurationDTO dto) {
        log.info("Updating rule with id: {}", id);
        
        RuleConfiguration existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found with id: " + id));
        
        // Check if rule name is being changed and if new name already exists
        if (!existing.getRuleName().equals(dto.getRuleName())) {
            if (repository.findByRuleName(dto.getRuleName()).isPresent()) {
                throw new IllegalArgumentException("Rule with name " + dto.getRuleName() + " already exists");
            }
        }
        
        // Update fields
        existing.setRuleName(dto.getRuleName());
        existing.setRuleType(dto.getRuleType());
        existing.setDescription(dto.getDescription());
        existing.setThresholdValue(dto.getThresholdValue());
        existing.setOperator(dto.getOperator());
        existing.setEnabled(dto.getEnabled());
        existing.setPriority(dto.getPriority());
        existing.setImportance(dto.getImportance());
        existing.setFailureMessage(dto.getFailureMessage());
        existing.setUpdatedBy(dto.getUpdatedBy());
        existing.setSource(dto.getSource() != null ? dto.getSource() : existing.getSource());
        existing.setConfidenceScore(dto.getConfidenceScore());
        existing.setModelVersion(dto.getModelVersion());
        existing.setMetadata(dto.getMetadata());
        
        RuleConfiguration saved = repository.save(existing);
        log.info("Rule updated successfully: {}", saved.getRuleName());
        
        return toDTO(saved);
    }
    
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RuleConfigurationDTO saveModelGeneratedRule(RuleConfigurationDTO dto) {
        log.info("Saving model-generated rule: {}", dto.getRuleName());
        RuleConfiguration rule = repository.findByRuleName(dto.getRuleName())
                .orElseGet(RuleConfiguration::new);
        
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setDescription(dto.getDescription());
        rule.setThresholdValue(dto.getThresholdValue());
        rule.setOperator(dto.getOperator());
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        rule.setImportance(dto.getImportance() != null ? dto.getImportance() : "HIGH");
        rule.setFailureMessage(dto.getFailureMessage());
        rule.setUpdatedBy(dto.getUpdatedBy());
        rule.setSource(dto.getSource() != null ? dto.getSource() : "MODEL");
        rule.setConfidenceScore(dto.getConfidenceScore());
        rule.setModelVersion(dto.getModelVersion());
        rule.setMetadata(dto.getMetadata());
        
        RuleConfiguration saved = repository.save(rule);
        log.info("Model-generated rule persisted: {}", saved.getRuleName());
        return toDTO(saved);
    }
    
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteRule(Long id) {
        log.info("Deleting rule with id: {}", id);
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Rule not found with id: " + id);
        }
        repository.deleteById(id);
        log.info("Rule deleted successfully");
    }
    
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RuleConfigurationDTO toggleRule(Long id, Boolean enabled) {
        log.info("Toggling rule {} to {}", id, enabled);
        RuleConfiguration rule = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found with id: " + id));
        rule.setEnabled(enabled);
        RuleConfiguration saved = repository.save(rule);
        return toDTO(saved);
    }
    
    private RuleConfiguration toEntity(RuleConfigurationDTO dto) {
        RuleConfiguration entity = new RuleConfiguration();
        entity.setRuleName(dto.getRuleName());
        entity.setRuleType(dto.getRuleType());
        entity.setDescription(dto.getDescription());
        entity.setThresholdValue(dto.getThresholdValue());
        entity.setOperator(dto.getOperator());
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        entity.setImportance(dto.getImportance() != null ? dto.getImportance() : "CRITICAL");
        entity.setFailureMessage(dto.getFailureMessage());
        entity.setUpdatedBy(dto.getUpdatedBy());
        entity.setSource(dto.getSource() != null ? dto.getSource() : "MANUAL");
        entity.setConfidenceScore(dto.getConfidenceScore());
        entity.setModelVersion(dto.getModelVersion());
        entity.setMetadata(dto.getMetadata());
        return entity;
    }
    
    private RuleConfigurationDTO toDTO(RuleConfiguration entity) {
        RuleConfigurationDTO dto = new RuleConfigurationDTO();
        dto.setId(entity.getId());
        dto.setRuleName(entity.getRuleName());
        dto.setRuleType(entity.getRuleType());
        dto.setDescription(entity.getDescription());
        dto.setThresholdValue(entity.getThresholdValue());
        dto.setOperator(entity.getOperator());
        dto.setEnabled(entity.getEnabled());
        dto.setPriority(entity.getPriority());
        dto.setImportance(entity.getImportance());
        dto.setFailureMessage(entity.getFailureMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setSource(entity.getSource());
        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setModelVersion(entity.getModelVersion());
        dto.setMetadata(entity.getMetadata());
        return dto;
    }
}


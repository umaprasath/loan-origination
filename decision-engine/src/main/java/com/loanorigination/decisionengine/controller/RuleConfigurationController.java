package com.loanorigination.decisionengine.controller;

import com.loanorigination.common.dto.RuleConfigurationDTO;
import com.loanorigination.decisionengine.service.RuleConfigurationService;
import com.loanorigination.decisionengine.service.RuleInferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rule Configuration", description = "API for managing credit decisioning rules")
public class RuleConfigurationController {
    
    private final RuleConfigurationService ruleConfigurationService;
    private final RuleInferenceService ruleInferenceService;
    
    public RuleConfigurationController(RuleConfigurationService ruleConfigurationService,
                                       RuleInferenceService ruleInferenceService) {
        this.ruleConfigurationService = ruleConfigurationService;
        this.ruleInferenceService = ruleInferenceService;
    }
    
    @Operation(
            summary = "Get all rules",
            description = "Retrieves all credit decisioning rules (both enabled and disabled)"
    )
    @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    @GetMapping
    public ResponseEntity<List<RuleConfigurationDTO>> getAllRules() {
        return ResponseEntity.ok(ruleConfigurationService.getAllRules());
    }
    
    @Operation(
            summary = "Get rule by ID",
            description = "Retrieves a specific rule by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RuleConfigurationDTO> getRuleById(
            @Parameter(description = "Rule ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ruleConfigurationService.getRuleById(id));
    }
    
    @Operation(
            summary = "Create a new rule",
            description = "Creates a new credit decisioning rule. The rule will be enabled by default."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or rule name already exists")
    })
    @PostMapping
    public ResponseEntity<RuleConfigurationDTO> createRule(@Valid @RequestBody RuleConfigurationDTO dto) {
        RuleConfigurationDTO created = ruleConfigurationService.createRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @Operation(
            summary = "Update an existing rule",
            description = "Updates an existing credit decisioning rule. All fields can be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RuleConfigurationDTO> updateRule(
            @Parameter(description = "Rule ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RuleConfigurationDTO dto) {
        return ResponseEntity.ok(ruleConfigurationService.updateRule(id, dto));
    }
    
    @Operation(
            summary = "Delete a rule",
            description = "Permanently deletes a credit decisioning rule"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Rule ID", example = "1")
            @PathVariable Long id) {
        ruleConfigurationService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "Enable or disable a rule",
            description = "Toggles the enabled status of a rule without deleting it"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RuleConfigurationDTO> toggleRule(
            @Parameter(description = "Rule ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Enable status", example = "true")
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(ruleConfigurationService.toggleRule(id, enabled));
    }
    
    @Operation(
            summary = "Infer rules from historical decisions",
            description = "Uses the configured LLM provider to infer new decision rules based on recent historical decisions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule inference completed"),
            @ApiResponse(responseCode = "503", description = "Rule inference unavailable")
    })
    @PostMapping("/infer/run")
    public ResponseEntity<RuleInferenceService.RuleInferenceResult> inferRules(
            @Parameter(description = "Number of recent decisions to analyze", example = "50")
            @RequestParam(name = "sampleSize", defaultValue = "50") int sampleSize) {
        RuleInferenceService.RuleInferenceResult result = ruleInferenceService.inferRules(sampleSize);
        return ResponseEntity.ok(result);
    }
}


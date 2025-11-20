package com.loanorigination.mcp.controller;

import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.dto.CreditResponse;
import com.loanorigination.common.dto.DecisionReasoning;
import com.loanorigination.mcp.service.McpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP Tools", description = "Model Context Protocol tools for AI-driven loan origination orchestration")
public class McpToolController {
    
    private final McpService mcpService;
    
    public McpToolController(McpService mcpService) {
        this.mcpService = mcpService;
    }
    
    @Operation(
            summary = "Credit Check Tool",
            description = "Performs a credit check and loan decision evaluation. " +
                    "This tool orchestrates calls to credit bureaus and the decision engine."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit check completed successfully",
                    content = @Content(schema = @Schema(implementation = CreditResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/tools/credit_check")
    public Mono<ResponseEntity<CreditResponse>> creditCheck(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credit check request with applicant and loan details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreditRequest.class))
            )
            @RequestBody CreditRequest request) {
        return mcpService.performCreditCheck(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
    
    @Operation(
            summary = "Get Decision Reasoning Tool",
            description = "Retrieves detailed reasoning for a previous loan decision. " +
                    "Useful for understanding why a loan was approved or rejected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reasoning retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DecisionReasoning.class))),
            @ApiResponse(responseCode = "404", description = "Reasoning not found for the given request ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tools/decision_reasoning/{requestId}")
    public Mono<ResponseEntity<DecisionReasoning>> getDecisionReasoning(
            @Parameter(description = "Request ID from a previous credit check", required = true)
            @PathVariable String requestId) {
        return mcpService.getDecisionReasoning(requestId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @Operation(
            summary = "Check LLM Status Tool",
            description = "Checks the status and availability of the LLM service. " +
                    "Useful for determining if AI-powered decision making is available."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "LLM status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tools/llm_status")
    public Mono<ResponseEntity<Map<String, Object>>> checkLlmStatus() {
        return mcpService.checkLlmStatus()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
    
    @Operation(
            summary = "Log Audit Event Tool",
            description = "Logs an audit event for compliance and tracking purposes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit event logged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/tools/audit_log")
    public Mono<ResponseEntity<Void>> logAuditEvent(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Audit event details",
                    required = true
            )
            @RequestBody Map<String, Object> auditRequest) {
        String requestId = (String) auditRequest.get("requestId");
        String serviceName = (String) auditRequest.get("serviceName");
        String action = (String) auditRequest.get("action");
        Object details = auditRequest.get("details");
        
        return mcpService.logAuditEvent(requestId, serviceName, action, details)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
    
    @Operation(
            summary = "Health Check",
            description = "Checks if the MCP server is running and healthy."
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mcp-server",
                "version", "1.0.0"
        ));
    }
}



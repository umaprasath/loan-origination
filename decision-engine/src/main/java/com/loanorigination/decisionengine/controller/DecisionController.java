package com.loanorigination.decisionengine.controller;

import com.loanorigination.common.dto.DecisionReasoning;
import com.loanorigination.common.dto.DecisionRequest;
import com.loanorigination.common.dto.DecisionResult;
import com.loanorigination.decisionengine.service.DecisionService;
import com.loanorigination.decisionengine.service.HybridDecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decision")
@Tag(name = "Decision Engine", description = "API for loan decision evaluation and reasoning")
public class DecisionController {
    
    private final HybridDecisionService hybridDecisionService;
    private final DecisionService decisionService;
    
    public DecisionController(HybridDecisionService hybridDecisionService, 
                            DecisionService decisionService) {
        this.hybridDecisionService = hybridDecisionService;
        this.decisionService = decisionService;
    }

    @Operation(
            summary = "Evaluate loan decision",
            description = "Evaluates a loan application based on credit bureau responses and loan amount, " +
                          "returning a decision with detailed reasoning. " +
                          "Supports multiple decision modes: 'rules' (rule-based only), 'llm' (LLM-based only), " +
                          "or 'hybrid' (both must agree). The decision mode is configured via 'decision.mode' property."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decision evaluated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "503", description = "LLM service unavailable (if LLM mode is enabled)")
    })
    @PostMapping("/evaluate")
    public ResponseEntity<DecisionResult> evaluate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Loan decision request with credit bureau responses, loan amount, and optional financial data (income, debt, cashflow)",
                    required = true
            )
            @RequestBody DecisionRequest request) {
        DecisionResult result = hybridDecisionService.evaluate(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "Get decision reasoning",
            description = "Retrieves detailed reasoning explaining how a decision was made for a given request ID. " +
                          "Includes rule evaluations, input values, calculated metrics, and decision path. " +
                          "Works for both rule-based and LLM-based decisions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reasoning retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DecisionReasoning.class))
            ),
            @ApiResponse(responseCode = "404", description = "Decision not found for the given request ID")
    })
    @GetMapping("/reasoning/{requestId}")
    public ResponseEntity<DecisionReasoning> getReasoning(
            @Parameter(
                    description = "Request ID of the decision to retrieve reasoning for",
                    example = "a615ad19-9ab5-482f-b0ae-7d323d7287d4",
                    required = true
            )
            @PathVariable String requestId) {
        DecisionReasoning reasoning = decisionService.getReasoning(requestId);
        return ResponseEntity.ok(reasoning);
    }
}


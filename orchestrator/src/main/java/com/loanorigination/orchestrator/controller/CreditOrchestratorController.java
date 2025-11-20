package com.loanorigination.orchestrator.controller;

import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.dto.CreditResponse;
import com.loanorigination.orchestrator.service.OrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/credit")
@Tag(name = "Credit Check", description = "API for credit check and loan origination")
public class CreditOrchestratorController {
    
    private final OrchestrationService orchestrationService;
    
    public CreditOrchestratorController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Operation(
            summary = "Check credit and get loan decision",
            description = "Submits a credit check request to multiple credit bureaus (Experian and Equifax) " +
                    "in parallel, evaluates the credit score, and returns a loan approval decision. " +
                    "The service implements a scatter-gather pattern for parallel credit bureau calls."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Credit check completed successfully",
                    content = @Content(schema = @Schema(implementation = CreditResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping(value = "/check", produces = "application/json", consumes = "application/json")
    public ResponseEntity<CreditResponse> checkCredit(@Valid @RequestBody CreditRequest request) {
        CreditResponse response = orchestrationService.processCreditCheck(request);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(response);
    }
}


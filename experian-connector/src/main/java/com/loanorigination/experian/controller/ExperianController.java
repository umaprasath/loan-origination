package com.loanorigination.experian.controller;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.experian.service.ExperianService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experian")
public class ExperianController {
    
    private final ExperianService experianService;
    
    public ExperianController(ExperianService experianService) {
        this.experianService = experianService;
    }

    @PostMapping("/check")
    public ResponseEntity<BureauResponse> checkCredit(@RequestBody CreditRequest request) {
        BureauResponse response = experianService.checkCredit(request);
        return ResponseEntity.ok(response);
    }
}


package com.loanorigination.equifax.controller;

import com.loanorigination.common.dto.BureauResponse;
import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.equifax.service.EquifaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equifax")
public class EquifaxController {
    
    private final EquifaxService equifaxService;
    
    public EquifaxController(EquifaxService equifaxService) {
        this.equifaxService = equifaxService;
    }

    @PostMapping("/check")
    public ResponseEntity<BureauResponse> checkCredit(@RequestBody CreditRequest request) {
        BureauResponse response = equifaxService.checkCredit(request);
        return ResponseEntity.ok(response);
    }
}


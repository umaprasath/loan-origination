package com.loanorigination.sms.controller;

import com.loanorigination.common.dto.CreditRequest;
import com.loanorigination.common.dto.CreditResponse;
import com.loanorigination.sms.service.NlpParserService;
import com.loanorigination.sms.service.OrchestratorClient;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/twilio")
public class TwilioWebhookController {
    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);

    private final NlpParserService parser;
    private final OrchestratorClient orchestratorClient;

    public TwilioWebhookController(NlpParserService parser, OrchestratorClient orchestratorClient) {
        this.parser = parser;
        this.orchestratorClient = orchestratorClient;
    }

    @PostMapping(value = "/sms", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> receiveSms(@RequestBody MultiValueMap<String, String> form) {
        String from = form.getFirst("From");
        String body = form.getFirst("Body");
        log.info("Received SMS from {}: {}", from, body);

        Map<String, Object> data = parser.parse(body != null ? body : "");
        if (!data.containsKey("loanAmount")) {
            return Mono.just(twiml("Please include a loan amount, e.g., 'loan $10000 term 24 months income 60000'"));
        }

        CreditRequest req = toCreditRequest(data);
        return orchestratorClient.submitCreditRequest(req)
                .map(orchestratorClient::summarize)
                .map(this::twiml)
                .onErrorResume(e -> {
                    log.error("Error processing SMS: {}", e.getMessage());
                    return Mono.just(twiml("Sorry, we couldn't process your request right now. Please try again later."));
                });
    }

    private CreditRequest toCreditRequest(Map<String, Object> data) {
        CreditRequest req = new CreditRequest();
        if (data.get("loanAmount") instanceof BigDecimal) {
            req.setLoanAmount(((BigDecimal) data.get("loanAmount")).doubleValue());
        }
        if (data.get("termMonths") instanceof Integer) {
            req.setTermMonths((Integer) data.get("termMonths"));
        }
        if (data.get("annualIncome") instanceof BigDecimal) {
            req.setAnnualIncome(((BigDecimal) data.get("annualIncome")).doubleValue());
        }
        if (data.get("existingDebt") instanceof BigDecimal) {
            req.setExistingDebt(((BigDecimal) data.get("existingDebt")).doubleValue());
        }
        if (data.get("creditScore") instanceof Integer) {
            req.setCreditScore((Integer) data.get("creditScore"));
        }
        if (data.get("applicantAge") instanceof Integer) {
            req.setApplicantAge((Integer) data.get("applicantAge"));
        }
        return req;
    }

    private String twiml(String msg) {
        Body body = new Body.Builder(msg).build();
        Message sms = new Message.Builder().body(body).build();
        MessagingResponse response = new MessagingResponse.Builder().message(sms).build();
        return response.toXml();
    }
}



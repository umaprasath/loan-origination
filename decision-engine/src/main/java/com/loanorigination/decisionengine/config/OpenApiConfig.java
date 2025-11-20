package com.loanorigination.decisionengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI decisionEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Decision Engine API")
                        .description("Loan Decision Engine Service with LLM Integration. " +
                                   "This service provides loan decisioning capabilities using both " +
                                   "rule-based and AI-powered (LLM) approaches. " +
                                   "Supports OpenAI and Ollama providers for LLM-based decisions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Loan Origination System")
                                .email("support@loanorigination.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}



package com.loanorigination.orchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI loanOriginationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Origination System API")
                        .description("API for loan origination and credit decisioning platform. " +
                                "This service orchestrates credit checks across multiple credit bureaus " +
                                "and provides loan approval decisions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Loan Origination Team")
                                .email("support@loanorigination.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}


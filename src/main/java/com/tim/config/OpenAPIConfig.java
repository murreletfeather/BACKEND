package com.tim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI meshProcessingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mesh Processing API")
                        .description("API for processing mesh data using QMorph algorithm")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Tim")
                                .email("your.email@example.com")));
    }
}
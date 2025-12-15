package com.wininger.spring_ai_demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring AI Demo API")
                        .version("1.0.0")
                        .description("API documentation for Spring AI Demo application")
                        .contact(new Contact()
                                .name("Chris Wininger")));
    }

    @Bean
    @ConfigurationPropertiesBinding
    public static ThinkOptionConverter thinkOptionConverter() {
        return new ThinkOptionConverter();
    }
}

package io.github.sueguo.finbridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("S3 File Manager API")
                        .version("0.1.0")
                        .description("REST API for managing file metadata backed by AWS S3")
                        .contact(new Contact().name("Demo Project")));
    }
}

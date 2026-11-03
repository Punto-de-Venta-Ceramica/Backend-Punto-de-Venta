package com.iaml.puntoventaapi.shared.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Habilita el botón "Authorize" de Swagger UI para enviar el JWT en el header Authorization. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}

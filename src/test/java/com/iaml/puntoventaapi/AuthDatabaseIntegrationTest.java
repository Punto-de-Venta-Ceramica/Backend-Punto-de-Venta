package com.iaml.puntoventaapi;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.devtools.restart.enabled=false")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_AUTH_DB_TESTS", matches = "true")
class AuthDatabaseIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilter;

    @Test
    void registerLoginDuplicateAndCorsWithRollback() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        String email = "integracion-" + UUID.randomUUID() + "@example.invalid";
        String registration = """
            {"nombre":"Prueba transaccional","apellidoPaterno":"Integracion","apellidoMaterno":"",
             "correo":"%s","telefono":"4181234567","password":"PruebaIntegracion1"}
            """.formatted(email);
        mvc.perform(options("/api/usuarios/registro")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(post("/api/usuarios/registro").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correo").exists());
        mvc.perform(post("/api/usuarios/registro").contentType("application/json").content(registration))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.correo").value(email))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/usuarios/registro").contentType("application/json").content(registration))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errores.correo").value("El correo electrónico ya está registrado."));
        mvc.perform(post("/api/usuarios/registro").contentType("application/json").content(registration.replace(email, email.toUpperCase(java.util.Locale.ROOT))))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"correo\":\"" + email + "\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
        var response = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"correo\":\"" + email + "\",\"password\":\"PruebaIntegracion1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.rol").value("PROPIETARIO"))
            .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");
        String claims = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]), java.nio.charset.StandardCharsets.UTF_8);
        Number issuedAt = JsonPath.read(claims, "$.iat");
        Number expiresAt = JsonPath.read(claims, "$.exp");
        org.junit.jupiter.api.Assertions.assertEquals(900L, expiresAt.longValue() - issuedAt.longValue());
        mvc.perform(get("/api/prueba-integracion-inexistente").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/prueba-integracion-inexistente")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").header("Origin", "https://untrusted.example")
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
}

package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SeguridadAutenticacionTest.Config.class)
@TestPropertySource(properties = "jwt.secreto=0123456789abcdef0123456789abcdef-secreto")
class SeguridadAutenticacionTest {

    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, Pruebas.class})
    static class Config {
    }

    @RestController
    static class Pruebas {
        @GetMapping("/api/protegido")
        String protegido() {
            return "ok";
        }

        @GetMapping("/api/auth/abierto")
        String abierto() {
            return "ok";
        }

        @PostMapping("/api/usuarios/registro")
        String registro() {
            return "ok";
        }
    }

    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtros;
    @Autowired JwtEncoder encoder;
    @Autowired PasswordEncoder passwordEncoder;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtros).build();
    }

    private String token(String tipo) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("7").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60)).claim("tipo", tipo)
            .claim("autoridades", List.of("ROLE_EMPLEADO")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }

    @Test
    void rutasPublicasNoPidenToken() throws Exception {
        mvc().perform(get("/api/auth/abierto")).andExpect(status().isOk());
        mvc().perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void laApiExigeTokenDeAcceso() throws Exception {
        mvc().perform(get("/api/protegido")).andExpect(status().isUnauthorized());
        mvc().perform(get("/api/protegido").header("Authorization", "Bearer " + token("acceso")))
            .andExpect(status().isOk());
        mvc().perform(get("/api/protegido").header("Authorization", "Bearer " + token("renovacion")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaUnSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig("corto"));
    }

    @Test
    void elHasherDelegaEnElCodificadorBCrypt() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(passwordEncoder);
        String hash = hasher.hashear("secreto");

        assertTrue(hasher.coincide("secreto", hash));
        assertFalse(hasher.coincide("otra", hash));
    }
}

package com.iaml.puntoventaapi.sucursal;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalJpaRepository;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.datasource.url=${TEST_DB_URL}",
    "spring.datasource.username=${TEST_DB_USERNAME}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=update",
    "jwt.secreto=clave-de-pruebas-locales-de-al-menos-32-bytes",
    "spring.devtools.restart.enabled=false"
})
@Transactional
class SucursalDatabaseIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired JwtEncoder encoder;
    @Autowired EntityManager em;
    @Autowired SucursalJpaRepository sucursales;

    private MockMvc mvc;
    private String bearer;
    private NegocioEntity negocio;
    private NegocioEntity negocioAjeno;
    private UsuarioEntity usuario;
    private UsuarioEntity otroUsuario;
    private Long sucursalAjenaId;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        RolEntity rol = em.createQuery("from RolEntity where nombre = 'PROPIETARIO'", RolEntity.class).getSingleResult();
        usuario = crearUsuario(rol, "uno@example.test");
        otroUsuario = crearUsuario(rol, "dos@example.test");
        negocio = crearNegocio(usuario, "Negocio propio");
        negocioAjeno = crearNegocio(otroUsuario, "Negocio ajeno");
        SucursalEntity ajena = new SucursalEntity();
        ajena.setNegocio(negocioAjeno);
        ajena.setNombre("Sucursal privada ajena");
        em.persist(ajena);
        em.flush();
        sucursalAjenaId = ajena.getId();
        bearer = token(usuario.getId().toString(), Instant.now().minusSeconds(5), Instant.now().plusSeconds(300));
    }

    @Test
    void registraEditaYEliminaUnaSucursalDelUsuarioAutenticado() throws Exception {
        long id = crearSucursal(negocio.getId());
        mvc.perform(patch("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":" + id + ",\"nombre\":\"Norte\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Norte"))
            .andExpect(jsonPath("$.descripcion").value("Original"))
            .andExpect(jsonPath("$.ciudad").value("Dolores"));
        em.clear();
        assertThat(sucursales.findById(id).orElseThrow().getNegocio().getId()).isEqualTo(negocio.getId());

        mvc.perform(patch("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":" + id + ",\"descripcion\":\"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value(""));
        mvc.perform(delete("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":" + id + "}"))
            .andExpect(status().isNoContent());
        assertThat(sucursales.findById(id)).isEmpty();
    }

    static Stream<Arguments> tokensNoValidos() {
        return Stream.of("POST", "GET", "PATCH", "DELETE", "CONSULTAR")
            .flatMap(metodo -> Stream.of("AUSENTE", "INVALIDO", "ALTERADO", "EXPIRADO", "SUB_INVALIDO")
                .map(tipo -> Arguments.of(metodo, tipo)));
    }

    @ParameterizedTest
    @MethodSource("tokensNoValidos")
    void todosLosEndpointsRechazanJwtAusenteOInvalido(String metodo, String tipo) throws Exception {
        long id = crearSucursal(negocio.getId());
        String body = switch (metodo) {
            case "POST" -> "{\"negocioId\":" + negocio.getId() + ",\"nombre\":\"No crear\"}";
            case "GET", "CONSULTAR" -> "{\"negocioId\":" + negocio.getId() + "}";
            case "PATCH" -> "{\"id\":" + id + ",\"nombre\":\"No editar\"}";
            default -> "{\"id\":" + id + "}";
        };
        var request = request(HttpMethod.valueOf(metodo.equals("CONSULTAR") ? "POST" : metodo),
            metodo.equals("CONSULTAR") ? "/api/sucursales/consultar" : "/api/sucursales")
            .contentType("application/json").content(body);
        String authorization = switch (tipo) {
            case "INVALIDO" -> "Bearer token-no-valido";
            case "ALTERADO" -> bearer.substring(0, bearer.lastIndexOf('.') + 1) + "AAAA";
            case "EXPIRADO" -> token(usuario.getId().toString(), Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
            case "SUB_INVALIDO" -> token("0", Instant.now().minusSeconds(5), Instant.now().plusSeconds(300));
            default -> null;
        };
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        long total = sucursales.count();
        mvc.perform(request).andExpect(status().isUnauthorized());
        em.clear();
        assertThat(sucursales.count()).isEqualTo(total);
        assertThat(sucursales.findById(id).orElseThrow().getNombre()).isEqualTo("Centro");
    }

    @Test
    void noPuedeConsultarCrearEditarNiEliminarSucursalesDeOtroUsuario() throws Exception {
        long total = sucursales.count();
        mvc.perform(post("/api/sucursales/consultar").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocioAjeno.getId() + "}"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("Sucursal privada ajena"))));
        mvc.perform(get("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocioAjeno.getId() + "}"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("Sucursal privada ajena"))));
        mvc.perform(post("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocioAjeno.getId() + ",\"nombre\":\"Intrusa\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":" + sucursalAjenaId + ",\"nombre\":\"Modificada\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":" + sucursalAjenaId + "}"))
            .andExpect(status().isNotFound());
        em.clear();
        assertThat(sucursales.count()).isEqualTo(total);
        assertThat(sucursales.findById(sucursalAjenaId).orElseThrow().getNombre()).isEqualTo("Sucursal privada ajena");
    }

    @Test
    void idsInexistentesYRecursosAjenosTienenLaMismaRespuesta() throws Exception {
        mvc.perform(post("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":9223372036854775807,\"nombre\":\"Centro\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"id\":9223372036854775807}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":9223372036854775807}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getFiltraPorNegocioYUsuario() throws Exception {
        long id = crearSucursal(negocio.getId());
        NegocioEntity otroPropio = crearNegocio(usuario, "Segundo negocio propio");
        crearSucursal(otroPropio.getId());
        mvc.perform(post("/api/sucursales/consultar").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocio.getId() + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value((int) id));
        mvc.perform(get("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocio.getId() + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value((int) id));
    }

    @Test
    void getDeUnNegocioPropioSinSucursalesDevuelveListaVacia() throws Exception {
        mvc.perform(get("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocio.getId() + "}"))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void enviarUsuarioIdEnElBodyNoPermiteSuplantarAlPropietario() throws Exception {
        mvc.perform(get("/api/sucursales").header("Authorization", bearer).contentType("application/json")
                .content("{\"negocioId\":" + negocioAjeno.getId() + ",\"usuarioId\":" + otroUsuario.getId() + "}"))
            .andExpect(status().isBadRequest());
    }

    private UsuarioEntity crearUsuario(RolEntity rol, String correo) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setRol(rol);
        entity.setNombre("Usuario de pruebas");
        entity.setCorreo(correo);
        entity.setPasswordHash("hash-exclusivo-de-pruebas");
        em.persist(entity);
        return entity;
    }

    private NegocioEntity crearNegocio(UsuarioEntity propietario, String nombre) {
        NegocioEntity entity = new NegocioEntity();
        entity.setNombre(nombre);
        entity.getUsuarios().add(propietario);
        em.persist(entity);
        em.flush();
        return entity;
    }

    private String token(String sub, Instant emitido, Instant expira) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(sub).issuedAt(emitido).expiresAt(expira)
            .claim("autoridades", List.of("ROLE_PROPIETARIO")).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private long crearSucursal(Long negocioId) throws Exception {
        String response = mvc.perform(post("/api/sucursales").header("Authorization", bearer)
                .contentType("application/json").content("""
                    {"negocioId":%d,"nombre":"Centro","descripcion":"Original",
                     "ciudad":"Dolores","calle":"Principal","codigoPostal":"37800"}
                    """.formatted(negocioId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}


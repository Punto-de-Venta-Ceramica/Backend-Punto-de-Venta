package com.iaml.puntoventaapi;

import com.iaml.puntoventaapi.autenticacion.api.AuthController;
import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionService;
import com.iaml.puntoventaapi.autenticacion.application.RenovarSesionService;
import com.iaml.puntoventaapi.shared.infrastructure.json.JsonConfig;
import com.iaml.puntoventaapi.sucursal.api.SucursalController;
import com.iaml.puntoventaapi.sucursal.application.SucursalService;
import com.iaml.puntoventaapi.usuarios.api.UsuarioController;
import com.iaml.puntoventaapi.usuarios.application.PropietarioService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {SucursalController.class, AuthController.class, UsuarioController.class},
    properties = {"spring.config.import=", "jwt.secreto=clave-de-pruebas-de-al-menos-32-bytes"})
@AutoConfigureMockMvc(addFilters = false)
@Import(JsonConfig.class)
class ErroresApiTest {

    @Autowired MockMvc mvc;
    @MockitoBean SucursalService sucursales;
    @MockitoBean IniciarSesionService sesiones;
    @MockitoBean RenovarSesionService renovaciones;
    @MockitoBean PropietarioService propietarios;

    static Stream<Arguments> endpoints() {
        return Stream.of(
            Arguments.of("POST", "/api/sucursales"),
            Arguments.of("PATCH", "/api/sucursales"),
            Arguments.of("DELETE", "/api/sucursales"),
            Arguments.of("GET", "/api/sucursales"),
            Arguments.of("POST", "/api/auth/login"),
            Arguments.of("POST", "/api/usuarios/registro"));
    }

    static Stream<Arguments> cuerposInvalidos() {
        return endpoints().flatMap(endpoint -> Stream.of("", "{", "null", "[]", "{} {}")
            .map(body -> Arguments.of(endpoint.get()[0], endpoint.get()[1], body)));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void bodyAusenteOJsonInvalidoDevuelve400SinInformacionInterna(String metodo, String ruta, String body)
            throws Exception {
        seguro(mvc.perform(request(HttpMethod.valueOf(metodo), ruta + "?trace=true")
            .contentType("application/json").content(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(sucursales, sesiones, propietarios);
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void tiposDeContenidoIncorrectosDevuelvenUnErrorSeguro(String metodo, String ruta) throws Exception {
        seguro(mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
            .contentType("text/plain").content("SENSITIVE_MARKER"))).andExpect(status().is4xxClientError());
        verifyNoInteractions(sucursales, sesiones, propietarios);
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void bodyAusenteSinContentTypeTambienDevuelveUnErrorSeguro(String metodo, String ruta) throws Exception {
        seguro(mvc.perform(request(HttpMethod.valueOf(metodo), ruta))).andExpect(status().isBadRequest());
        verifyNoInteractions(sucursales, sesiones, propietarios);
    }

    static Stream<Arguments> tiposInvalidos() {
        return Stream.of(
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":\"2\",\"nombre\":\"Centro\"}"),
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":2.5,\"nombre\":\"Centro\"}"),
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":true,\"nombre\":\"Centro\"}"),
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":2,\"nombre\":123}"),
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":2,\"descripcion\":false,\"nombre\":\"Centro\"}"),
            Arguments.of("PATCH", "/api/sucursales", "{\"id\":2,\"nombre\":123}"),
            Arguments.of("DELETE", "/api/sucursales", "{\"id\":\"2\"}"),
            Arguments.of("GET", "/api/sucursales", "{\"negocioId\":2.5}"),
            Arguments.of("GET", "/api/sucursales", "{\"negocioId\":{\"dato\":\"SENSITIVE_MARKER\"}}"),
            Arguments.of("POST", "/api/auth/login", "{\"correo\":true,\"password\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("POST", "/api/usuarios/registro", "{\"nombre\":{},\"password\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("POST", "/api/sucursales", "{\"negocioId\":2,\"nombre\":\"Centro\",\"campoInterno\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("PATCH", "/api/sucursales", "{\"id\":2,\"nombre\":\"Centro\",\"ciudad\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("DELETE", "/api/sucursales", "{\"id\":2,\"campoInterno\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("GET", "/api/sucursales", "{\"negocioId\":2,\"campoInterno\":\"SENSITIVE_MARKER\"}"),
            Arguments.of("POST", "/api/auth/login", "{\"correo\":\"a@example.com\",\"password\":\"SENSITIVE_MARKER\",\"extra\":1}"));
    }

    @ParameterizedTest
    @MethodSource("tiposInvalidos")
    void rechazaCoercionesYCamposDesconocidos(String metodo, String ruta, String body) throws Exception {
        seguro(mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
            .contentType("application/json").content(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(sucursales, sesiones, propietarios);
    }

    static Stream<Arguments> formatosInvalidos() {
        return Stream.of(
            Arguments.of("POST", "{\"negocioId\":0,\"nombre\":\"Centro\"}", "negocioId"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\\nNorte\"}", "nombre"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"codigoPostal\":\"3780\"}", "codigoPostal"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"codigoPostal\":\"ABCDE\"}", "codigoPostal"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"codigoPostal\":\"378001\"}", "codigoPostal"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"ciudad\":\"Dolores\\t\"}", "ciudad"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"calle\":\"Principal\\n\"}", "calle"),
            Arguments.of("POST", "{\"negocioId\":2,\"nombre\":\"Centro\",\"descripcion\":\"" + "A".repeat(2001) + "\"}", "descripcion"),
            Arguments.of("PATCH", "{\"id\":2,\"nombre\":\"Centro\\t\"}", "nombre"),
            Arguments.of("GET", "{\"negocioId\":0}", "negocioId"),
            Arguments.of("GET", "{}", "negocioId"),
            Arguments.of("PATCH", "{\"id\":2,\"descripcion\":\"" + "A".repeat(2001) + "\"}", "descripcion"));
    }

    @ParameterizedTest
    @MethodSource("formatosInvalidos")
    void validaFormatoDelDtoSinEnviarValoresRechazados(String metodo, String body, String campo) throws Exception {
        seguro(mvc.perform(request(HttpMethod.valueOf(metodo), "/api/sucursales")
            .contentType("application/json").content(body)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores." + campo).isString());
        verifyNoInteractions(sucursales);
    }

    @Test
    void unErrorInesperadoNoPublicaElMensajeTecnico() throws Exception {
        when(sucursales.crear(any())).thenThrow(new IllegalStateException(
            "SENSITIVE_MARKER com.iaml.puntoventaapi.interno SELECT * FROM usuario"));
        seguro(mvc.perform(post("/api/sucursales?trace=true").contentType("application/json")
            .content("{\"negocioId\":2,\"nombre\":\"Centro\"}")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Inténtalo más tarde."));
    }

    @Test
    void erroresDeBaseDeDatosNoPublicanSqlNiCausas() throws Exception {
        when(sucursales.crear(any())).thenThrow(new DataIntegrityViolationException("SENSITIVE_MARKER"));
        seguro(mvc.perform(post("/api/sucursales").contentType("application/json")
            .content("{\"negocioId\":2,\"nombre\":\"Centro\"}"))).andExpect(status().isConflict());
        doThrow(new DataAccessResourceFailureException("SENSITIVE_MARKER")).when(sucursales).crear(any());
        seguro(mvc.perform(post("/api/sucursales").contentType("application/json")
            .content("{\"negocioId\":2,\"nombre\":\"Centro\"}"))).andExpect(status().isServiceUnavailable());
    }

    @Test
    void validacionDeLoginNoReflejaLaContrasena() throws Exception {
        seguro(mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"correo\":\"correo-invalido\",\"password\":\"" + "SENSITIVE_MARKER".repeat(10) + "\"}")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    void permiteTextoConAcentosDescripcionMultilineaYCodigoPostalConCeroInicial() throws Exception {
        when(sucursales.crear(any())).thenReturn(
            new com.iaml.puntoventaapi.sucursal.domain.Sucursal(
                5L, 2L, "Cerámica Centro", "Dos líneas\nDescripción", "Dolores Hidalgo", "Principal 10", "03780"));
        mvc.perform(post("/api/sucursales").contentType("application/json")
                .content("{\"negocioId\":2,\"nombre\":\"Cerámica Centro\",\"descripcion\":\"Dos líneas\\nDescripción\",\"codigoPostal\":\"03780\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.codigoPostal").value("03780"));
    }

    @Test
    void rutasYMetodosNoDisponiblesTienenRespuestasSeguras() throws Exception {
        seguro(mvc.perform(put("/api/sucursales").contentType("application/json").content("{}")))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"));
        seguro(mvc.perform(get("/api/auth/ruta-inexistente?trace=true"))).andExpect(status().isNotFound());
    }

    private ResultActions seguro(ResultActions result) throws Exception {
        return result.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.cause").doesNotExist())
            .andExpect(content().string(not(containsString("SENSITIVE_MARKER"))))
            .andExpect(content().string(not(containsString("com.iaml."))))
            .andExpect(content().string(not(containsString("org.springframework."))))
            .andExpect(content().string(not(containsString("org.hibernate."))))
            .andExpect(content().string(not(containsString("SELECT *"))));
    }
}


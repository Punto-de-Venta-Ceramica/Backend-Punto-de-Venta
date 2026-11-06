package com.iaml.puntoventaapi.usuarios;

import com.iaml.puntoventaapi.shared.api.ManejadorGlobalExcepciones;
import com.iaml.puntoventaapi.usuarios.api.EmpleadoController;
import com.iaml.puntoventaapi.usuarios.application.*;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmpleadoControllerTest {

    private EmpleadoService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(EmpleadoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EmpleadoController(service))
            .setControllerAdvice(new ManejadorGlobalExcepciones())
            .build();
    }

    @Test
    void registrarDevuelve201YPasswordGenerada() throws Exception {
        Usuario usuario = new Usuario(10L, 2L, 5L, 20L, "Laura", "Perez", "Diaz", "laura@test.com", null, "hash");
        when(service.registrar(any())).thenReturn(new EmpleadoCreado(usuario, "PassSecret1!", "Sucursal Centro"));

        mvc.perform(post("/api/empleados")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "nombre": "Laura",
                        "apellidoP": "Perez",
                        "apellidoM": "Diaz",
                        "correoElectronico": "laura@test.com",
                        "sucursalId": 20
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.negocioId").value(5))
            .andExpect(jsonPath("$.nombre").value("Laura"))
            .andExpect(jsonPath("$.correoElectronico").value("laura@test.com"))
            .andExpect(jsonPath("$.passwordGenerada").value("PassSecret1!"))
            .andExpect(jsonPath("$.sucursalNombre").value("Sucursal Centro"));
    }

    @Test
    void registrarRechazaCamposInvalidos() throws Exception {
        // Nombre vacío y correo inválido
        mvc.perform(post("/api/empleados")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 0,
                        "nombre": "   ",
                        "correoElectronico": "correo-no-valido"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.negocioId").exists())
            .andExpect(jsonPath("$.errores.nombre").exists())
            .andExpect(jsonPath("$.errores.correoElectronico").exists());
        verifyNoInteractions(service);
    }

    @Test
    void registrarDevuelve409SiCorreoYaExiste() throws Exception {
        when(service.registrar(any())).thenThrow(new CorreoYaRegistradoException());

        mvc.perform(post("/api/empleados")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "nombre": "Laura",
                        "correoElectronico": "laura@test.com"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errores.correoElectronico").value("El correo electrónico ya está registrado."));
    }

    @Test
    void listarDevuelve200ConLista() throws Exception {
        Usuario usuario = new Usuario(10L, 2L, 5L, null, "Laura", "Perez", null, "laura@test.com", null, "hash");
        when(service.listarPorNegocio(5L, false)).thenReturn(List.of(new EmpleadoDetalle(usuario, null, null)));

        mvc.perform(get("/api/empleados?negocioId=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].nombre").value("Laura"));
    }

    @Test
    void consultarDesdeWebDevuelve200() throws Exception {
        Usuario usuario = new Usuario(10L, 2L, 5L, 20L, "Laura", "Perez", null, "laura@test.com", null, "hash");
        when(service.listarPorNegocio(5L, true)).thenReturn(List.of(new EmpleadoDetalle(usuario, "Sucursal Centro", true)));

        mvc.perform(post("/api/empleados/consultar")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "incluirAsignacion": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].sucursalId").value(20))
            .andExpect(jsonPath("$[0].sucursalNombre").value("Sucursal Centro"))
            .andExpect(jsonPath("$[0].asignado").value(true));
    }

    @Test
    void asignarSucursalDevuelve200() throws Exception {
        Usuario usuario = new Usuario(10L, 2L, 5L, 20L, "Laura", "Perez", null, "laura@test.com", null, "hash");
        when(service.asignarSucursal(any())).thenReturn(new EmpleadoDetalle(usuario, "Sucursal Norte", true));

        mvc.perform(patch("/api/empleados/asignar-sucursal")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "empleadoId": 10,
                        "sucursalId": 20
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sucursalId").value(20))
            .andExpect(jsonPath("$.asignado").value(true));
    }

    @Test
    void asignarSucursalDevuelve409SiYaTieneSucursal() throws Exception {
        when(service.asignarSucursal(any())).thenThrow(new EmpleadoYaTieneSucursalException("Ya asignado"));

        mvc.perform(patch("/api/empleados/asignar-sucursal")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "empleadoId": 10,
                        "sucursalId": 20
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya asignado"));
    }

    @Test
    void editarDevuelve200() throws Exception {
        Usuario usuario = new Usuario(10L, 2L, 5L, null, "Laura Elena", "Perez", "Diaz", "laura@test.com", null, "hash");
        when(service.editar(any())).thenReturn(new EmpleadoDetalle(usuario, null, false));

        mvc.perform(patch("/api/empleados")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "empleadoId": 10,
                        "nombre": "Laura Elena",
                        "apellidoP": "Perez",
                        "apellidoM": "Diaz"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Laura Elena"));
    }

    @Test
    void eliminarPorBodyDevuelve204() throws Exception {
        mvc.perform(delete("/api/empleados")
                .contentType("application/json")
                .content("""
                    {
                        "negocioId": 5,
                        "empleadoId": 10
                    }
                    """))
            .andExpect(status().isNoContent());

        verify(service).eliminar(any());
    }

    @Test
    void eliminarPorPathDevuelve204() throws Exception {
        mvc.perform(delete("/api/empleados/10?negocioId=5"))
            .andExpect(status().isNoContent());

        verify(service).eliminar(any());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        doThrow(new EmpleadoNoEncontradoException(99L)).when(service).eliminar(any());

        mvc.perform(delete("/api/empleados/99?negocioId=5"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Empleado no encontrado con id 99"));
    }
}


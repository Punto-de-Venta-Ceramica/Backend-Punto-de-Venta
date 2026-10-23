package com.iaml.puntoventaapi;

import com.iaml.puntoventaapi.usuarios.api.UsuarioController;
import com.iaml.puntoventaapi.usuarios.api.dto.RegistrarPropietarioRequest;
import com.iaml.puntoventaapi.usuarios.application.PropietarioService;
import com.iaml.puntoventaapi.shared.api.ManejadorGlobalExcepciones;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ValidacionRegistroTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private RegistrarPropietarioRequest request(String nombre, String correo, String telefono) {
        return new RegistrarPropietarioRequest(nombre, "Pérez", "", correo, telefono, "Registro123");
    }
    private void message(RegistrarPropietarioRequest input, String field, String message) {
        assertTrue(VALIDATOR.validate(input).stream().anyMatch(v ->
            v.getPropertyPath().toString().equals(field) && v.getMessage().equals(message)));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n"})
    void textosObligatorios(String blank) {
        message(request(blank, "a@ejemplo.com", "4181234567"), "nombre", "El nombre es obligatorio y no puede contener solo espacios.");
        message(request("Ana", blank, "4181234567"), "correo", "El correo electrónico es obligatorio.");
        message(request("Ana", "a@ejemplo.com", blank), "telefono", "El teléfono es obligatorio.");
    }
    @ParameterizedTest @ValueSource(strings = {"123456789", "12345678901", "abcdefghij", "123-456789", "+521234567890", " 1234567890"})
    void telefonoExactamenteDiezDigitos(String phone) {
        message(request("Ana", "a@ejemplo.com", phone), "telefono", "El teléfono debe contener exactamente 10 dígitos.");
    }
    @Test void correoYLongitudes() {
        assertTrue(VALIDATOR.validate(request("Ana", "a@ejemplo.com", "4181234567")).isEmpty());
        message(request("Ana", "correo@", "4181234567"), "correo", "El correo electrónico no tiene un formato válido.");
        message(request("A".repeat(101), "a@ejemplo.com", "4181234567"), "nombre", "El nombre no debe superar 100 caracteres.");
    }
    @Test void validActivaValidacionYNoLlamaAlServicio() throws Exception {
        var service = mock(PropietarioService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service))
            .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
        mvc.perform(post("/api/usuarios/registro").contentType("application/json")
            .content("{\"nombre\":\"   \",\"correo\":\"\",\"telefono\":\"\",\"password\":\"Registro123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio y no puede contener solo espacios."))
            .andExpect(jsonPath("$.errores.correo").value("El correo electrónico es obligatorio."))
            .andExpect(jsonPath("$.errores.telefono").value("El teléfono es obligatorio."));
        verifyNoInteractions(service);
    }
}

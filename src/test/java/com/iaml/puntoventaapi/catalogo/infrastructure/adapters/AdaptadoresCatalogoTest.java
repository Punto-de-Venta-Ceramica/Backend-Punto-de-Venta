package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.iaml.puntoventaapi.inventario.InventarioContrato;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptadoresCatalogoTest {

    @Test
    void inventarioAdapterDelegaEnElContrato() {
        InventarioContrato contrato = mock(InventarioContrato.class);

        new InventarioAdapter(contrato).registrarExistenciasIniciales(5L, Map.of(1L, 2));

        verify(contrato).registrarExistenciasIniciales(5L, Map.of(1L, 2));
    }

    @Test
    void negocioAdapterDelegaEnLosContratos() {
        NegociosContrato negocios = mock(NegociosContrato.class);
        SucursalContrato sucursales = mock(SucursalContrato.class);
        when(negocios.perteneceAUsuario(1L, 7L)).thenReturn(true);
        when(sucursales.negocioAutorizado(3L, 7L)).thenReturn(Optional.of(1L));
        NegocioAdapter adapter = new NegocioAdapter(negocios, sucursales);

        assertTrue(adapter.perteneceAUsuario(1L, 7L));
        assertEquals(Optional.of(1L), adapter.negocioDeSucursal(3L, 7L));
    }

    @Test
    void sucursalAdapterDelegaEnElContrato() {
        SucursalContrato sucursales = mock(SucursalContrato.class);
        when(sucursales.idsPorNegocio(1L, 7L)).thenReturn(List.of(3L));

        assertEquals(List.of(3L), new SucursalAdapter(sucursales).idsPorNegocio(1L, 7L));
    }

    // ---------- Cloudinary ----------

    @Test
    void sinCredencialesNoIntentaBorrar() {
        CloudinaryAlmacenImagenesAdapter sinNada = new CloudinaryAlmacenImagenesAdapter(null, null, null);
        CloudinaryAlmacenImagenesAdapter vacias = new CloudinaryAlmacenImagenesAdapter("", "  ", "x");
        CloudinaryAlmacenImagenesAdapter sinCloudinary = new CloudinaryAlmacenImagenesAdapter((Cloudinary) null);

        CloudinaryAlmacenImagenesAdapter sinSecreto = new CloudinaryAlmacenImagenesAdapter("n", "k", " ");
        CloudinaryAlmacenImagenesAdapter sinLlave = new CloudinaryAlmacenImagenesAdapter("n", "", "s");

        assertDoesNotThrow(() -> sinSecreto.eliminar("pid"));
        assertDoesNotThrow(() -> sinLlave.eliminar("pid"));
        assertDoesNotThrow(() -> sinNada.eliminar("pid"));
        assertDoesNotThrow(() -> vacias.eliminar("pid"));
        assertDoesNotThrow(() -> sinCloudinary.eliminar("pid"));
        assertDoesNotThrow(() -> sinNada.eliminar(null));
    }

    @Test
    void conCredencialesConstruyeElClienteYSeToleraLaFaltaDeRed() {
        CloudinaryAlmacenImagenesAdapter adapter =
            new CloudinaryAlmacenImagenesAdapter(" nube ", " llave ", " secreto ");

        // Sin red real, la llamada a Cloudinary falla y el adaptador lo absorbe.
        assertDoesNotThrow(() -> adapter.eliminar("pid"));
    }

    @Test
    void registraLasRespuestasInesperadasSinFallar() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        Map<String, Object> error = new HashMap<>();
        error.put("result", "error");
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "not found"), error, null);
        CloudinaryAlmacenImagenesAdapter adapter = new CloudinaryAlmacenImagenesAdapter(cloudinary);

        adapter.eliminar("a"); // not found: aceptable
        adapter.eliminar("b"); // otra respuesta: se registra el error
        adapter.eliminar("c"); // respuesta nula

        verify(uploader, org.mockito.Mockito.times(3)).destroy(anyString(), anyMap());
        verify(uploader, never()).destroy(org.mockito.ArgumentMatchers.eq(""), anyMap());
    }
}

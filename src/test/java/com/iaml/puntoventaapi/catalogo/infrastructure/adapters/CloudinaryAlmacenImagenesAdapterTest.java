package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryAlmacenImagenesAdapterTest {

    @Test
    void eliminaImagenEnCloudinaryConPublicIdLimpio() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("productos/jarron_1"), anyMap())).thenReturn(Map.of("result", "ok"));

        CloudinaryAlmacenImagenesAdapter adapter = new CloudinaryAlmacenImagenesAdapter(cloudinary);
        adapter.eliminar("  productos/jarron_1  ");

        verify(uploader).destroy(eq("productos/jarron_1"), anyMap());
    }

    @Test
    void ignoraPublicIdNuloOVacio() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        CloudinaryAlmacenImagenesAdapter adapter = new CloudinaryAlmacenImagenesAdapter(cloudinary);
        adapter.eliminar(null);
        adapter.eliminar("   ");

        verify(uploader, never()).destroy(eq(""), anyMap());
    }

    @Test
    void siCloudinaryFallaNoPropagaExcepcion() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("productos/jarron_1"), anyMap())).thenThrow(new IOException("Fallo de red"));

        CloudinaryAlmacenImagenesAdapter adapter = new CloudinaryAlmacenImagenesAdapter(cloudinary);

        assertDoesNotThrow(() -> adapter.eliminar("productos/jarron_1"));
    }
}


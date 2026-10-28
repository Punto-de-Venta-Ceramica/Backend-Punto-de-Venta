package com.iaml.puntoventaapi.negocios.infrastructure.imagen;

import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;
import com.iaml.puntoventaapi.negocios.domain.port.ProcesadorLogoPort;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

@Component
public class ProcesadorLogo implements ProcesadorLogoPort {

    private static final long MAX_PIXELES = 16_000_000;

    @Override
    public String procesar(LogoArchivo logo) {
        if (logo == null) {
            return null;
        }
        byte[] contenido = logo.getContenido();
        String tipoImagen = validarImagen(contenido);
        return "data:" + tipoImagen + ";base64," + Base64.getEncoder().encodeToString(contenido);
    }

    private String validarImagen(byte[] contenido) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(input);
            if (!lectores.hasNext()) {
                throw new LogoInvalidoException("El contenido del logo no es una imagen valida");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(input, true, true);
                String tipoImagen = obtenerTipoImagen(lector.getFormatName());
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles > MAX_PIXELES) {
                    throw new LogoInvalidoException("El logo no debe superar 16 millones de pixeles");
                }
                lector.read(0);
                return tipoImagen;
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new LogoInvalidoException("No se pudo leer la imagen del logo");
        }
    }

    private String obtenerTipoImagen(String formato) {
        return switch (formato.toUpperCase(java.util.Locale.ROOT)) {
            case "PNG" -> "image/png";
            case "JPEG" -> "image/jpeg";
            default -> throw new LogoInvalidoException("El logo debe ser una imagen PNG o JPEG");
        };
    }
}

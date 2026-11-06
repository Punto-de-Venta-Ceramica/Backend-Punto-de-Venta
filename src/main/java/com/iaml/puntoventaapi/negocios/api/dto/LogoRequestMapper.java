package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;
import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

final class LogoRequestMapper {

    private LogoRequestMapper() {
    }

    static LogoArchivo toDomain(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > LogoArchivo.MAX_BYTES) {
            throw new LogoInvalidoException("El logo no debe superar 2 MB");
        }
        try {
            return new LogoArchivo(archivo.getBytes());
        } catch (IOException ex) {
            throw new LogoInvalidoException("No se pudo leer el archivo del logo");
        }
    }
}

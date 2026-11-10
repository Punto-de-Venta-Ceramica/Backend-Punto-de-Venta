package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.iaml.puntoventaapi.catalogo.domain.port.AlmacenImagenesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CloudinaryAlmacenImagenesAdapter implements AlmacenImagenesPort {

    private static final Logger LOG = LoggerFactory.getLogger(CloudinaryAlmacenImagenesAdapter.class);

    private final Cloudinary cloudinary;
    private final boolean configurado;

    @Autowired
    public CloudinaryAlmacenImagenesAdapter(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.configurado = noVacio(cloudName) && noVacio(apiKey) && noVacio(apiSecret);
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName == null ? "" : cloudName.trim(),
            "api_key", apiKey == null ? "" : apiKey.trim(),
            "api_secret", apiSecret == null ? "" : apiSecret.trim(),
            "secure", true
        ));
    }

    public CloudinaryAlmacenImagenesAdapter(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
        this.configurado = cloudinary != null;
    }

    @Override
    public void eliminar(String publicId) {
        if (!noVacio(publicId)) {
            return;
        }
        if (!configurado) {
            LOG.error("No se pudo eliminar la imagen '{}' en Cloudinary: faltan las credenciales de configuración.", publicId);
            return;
        }
        try {
            Map<?, ?> resultado = cloudinary.uploader().destroy(publicId.trim(), ObjectUtils.asMap("invalidate", true));
            Object estado = resultado == null ? null : resultado.get("result");
            if (!"ok".equals(estado) && !"not found".equals(estado)) {
                LOG.error("Cloudinary no confirmó el borrado de la imagen '{}'. Respuesta: {}", publicId, estado);
            }
        } catch (Exception ex) {
            LOG.error("Error al eliminar la imagen '{}' en Cloudinary.", publicId, ex);
        }
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }
}


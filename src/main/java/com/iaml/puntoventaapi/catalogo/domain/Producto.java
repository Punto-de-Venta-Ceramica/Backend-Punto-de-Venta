package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

public final class Producto {

    private static final int NOMBRE_MAX = 150;
    private static final BigDecimal PRECIO_MAX = new BigDecimal("9999999999.99");
    private static final String DOMINIO_CLOUDINARY = "res.cloudinary.com";

    private final Long id;
    private Long categoriaId;
    private String nombre;
    private BigDecimal precio;
    private String imagenUrl;
    private String imagenPublicId;
    private boolean activo;

    public Producto(Long id, Long categoriaId, String nombre, BigDecimal precio, boolean activo) {
        this(id, categoriaId, nombre, precio, null, null, activo);
    }

    public Producto(Long id, Long categoriaId, String nombre, BigDecimal precio,
                    String imagenUrl, String imagenPublicId, boolean activo) {
        this.id = id;
        setCategoriaId(categoriaId);
        setNombre(nombre);
        setPrecio(precio);
        setImagenUrl(imagenUrl);
        setImagenPublicId(imagenPublicId);
        this.activo = activo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        if (categoriaId == null || categoriaId <= 0) {
            throw new IllegalArgumentException("La categoría debe tener un identificador positivo.");
        }
        this.categoriaId = categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > NOMBRE_MAX) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio y no debe superar " + NOMBRE_MAX + " caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0 || precio.scale() > 2 || precio.compareTo(PRECIO_MAX) > 0) {
            throw new IllegalArgumentException(
                "El precio es obligatorio, no puede ser negativo y admite máximo 2 decimales.");
        }
        this.precio = precio;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        if (imagenUrl == null) {
            this.imagenUrl = null;
            return;
        }
        String limpia = imagenUrl.trim();
        if (limpia.isEmpty() || !esUrlCloudinaryValida(limpia)) {
            throw new ProductoInvalidoException(
                "La URL de la imagen debe usar https y pertenecer al dominio res.cloudinary.com.");
        }
        this.imagenUrl = limpia;
    }

    public String getImagenPublicId() {
        return imagenPublicId;
    }

    public void setImagenPublicId(String imagenPublicId) {
        if (imagenPublicId == null || imagenPublicId.isBlank()) {
            this.imagenPublicId = null;
            return;
        }
        String limpio = imagenPublicId.trim();
        if (limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new ProductoInvalidoException(
                "El identificador público de la imagen no debe contener caracteres de control.");
        }
        this.imagenPublicId = limpio;
    }

    public boolean isActivo() {
        return activo;
    }

    private static boolean esUrlCloudinaryValida(String url) {
        if (url.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c))) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                && uri.getUserInfo() == null
                && DOMINIO_CLOUDINARY.equalsIgnoreCase(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().length() > 1;
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}

package com.iaml.puntoventaapi.negocios.infrastructure.imagen;

import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;
import com.iaml.puntoventaapi.negocios.domain.port.ProcesadorLogoPort;
import org.springframework.stereotype.Component;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

@Component
public class ProcesadorLogo implements ProcesadorLogoPort {

    private static final long MAX_PIXELES = 16_000_000;
    /** Lado mayor con el que se guarda el logo: suficiente para la app y el ticket de 58 mm. */
    static final int LADO_MAXIMO = 512;
    private static final float CALIDAD_JPEG = 0.85f;

    private record ImagenLeida(String tipo, BufferedImage imagen) { }

    @Override
    public String procesar(LogoArchivo logo) {
        if (logo == null) {
            return null;
        }
        byte[] contenido = logo.getContenido();
        ImagenLeida leida = validarImagen(contenido);
        byte[] guardar = reducir(leida, contenido);
        return "data:" + leida.tipo() + ";base64," + Base64.getEncoder().encodeToString(guardar);
    }

    /**
     * Escala el logo para que su lado mayor no pase de {@link #LADO_MAXIMO} y lo vuelve a codificar en su
     * mismo formato. Si ya es chico o el resultado no pesa menos, se guarda el original.
     */
    private byte[] reducir(ImagenLeida leida, byte[] original) {
        BufferedImage imagen = leida.imagen();
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        int mayor = Math.max(ancho, alto);
        if (mayor <= LADO_MAXIMO) {
            return original;
        }
        double escala = (double) LADO_MAXIMO / mayor;
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));
        boolean png = "image/png".equals(leida.tipo());
        BufferedImage reducida = new BufferedImage(nuevoAncho, nuevoAlto,
            png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = reducida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(imagen, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            g.dispose();
        }
        try {
            byte[] codificada = png ? codificarPng(reducida) : codificarJpeg(reducida);
            return codificada.length < original.length ? codificada : original;
        } catch (IOException ex) {
            return original;
        }
    }

    private static byte[] codificarPng(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagen, "png", salida)) {
            throw new IOException("Sin codificador PNG");
        }
        return salida.toByteArray();
    }

    private static byte[] codificarJpeg(BufferedImage imagen) throws IOException {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) {
            throw new IOException("Sin codificador JPEG");
        }
        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(out);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    private ImagenLeida validarImagen(byte[] contenido) {
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
                return new ImagenLeida(tipoImagen, lector.read(0));
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

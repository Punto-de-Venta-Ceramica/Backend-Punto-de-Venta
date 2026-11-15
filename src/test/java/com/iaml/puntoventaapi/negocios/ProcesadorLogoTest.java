package com.iaml.puntoventaapi.negocios;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;
import com.iaml.puntoventaapi.negocios.infrastructure.imagen.ProcesadorLogo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ProcesadorLogoTest {

    private final ProcesadorLogo procesador = new ProcesadorLogo();

    @Test
    void reduceUnPngGrandeA512ConservandoProporcionYTransparencia() throws Exception {
        byte[] original = imagen(1000, 500, BufferedImage.TYPE_INT_ARGB, "png");

        String dataUri = procesador.procesar(new LogoArchivo(original));

        assertTrue(dataUri.startsWith("data:image/png;base64,"));
        byte[] guardado = decodificar(dataUri);
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(guardado));
        assertEquals(512, leida.getWidth());
        assertEquals(256, leida.getHeight());
        assertTrue(leida.getColorModel().hasAlpha());
        assertTrue(guardado.length < original.length);
    }

    @Test
    void reduceUnJpegGrandeYLoMantieneEnJpeg() throws Exception {
        byte[] original = imagen(700, 1400, BufferedImage.TYPE_INT_RGB, "jpeg");

        String dataUri = procesador.procesar(new LogoArchivo(original));

        assertTrue(dataUri.startsWith("data:image/jpeg;base64,"));
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(decodificar(dataUri)));
        assertEquals(256, leida.getWidth());
        assertEquals(512, leida.getHeight());
    }

    @Test
    void unLogoChicoSeGuardaTalCual() throws Exception {
        byte[] original = imagen(200, 120, BufferedImage.TYPE_INT_ARGB, "png");

        String dataUri = procesador.procesar(new LogoArchivo(original));

        assertArrayEquals(original, decodificar(dataUri));
    }

    private static byte[] decodificar(String dataUri) {
        return Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
    }

    /** Imagen con ruido para que no se comprima trivialmente. */
    private static byte[] imagen(int ancho, int alto, int tipo, String formato) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, tipo);
        Random random = new Random(7);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                imagen.setRGB(x, y, 0xFF000000 | random.nextInt(0xFFFFFF));
            }
        }
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.BLACK);
        g.fillOval(ancho / 4, alto / 4, ancho / 2, alto / 2);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}

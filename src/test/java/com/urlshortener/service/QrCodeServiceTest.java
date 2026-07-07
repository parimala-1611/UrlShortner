package com.urlshortener.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void generatesPngThatDecodesBackToOriginalText() throws Exception {
        String encoded = "http://localhost:8080/abc12345";

        byte[] png = service.generatePng(encoded);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(encoded);
    }

    @Test
    void producesValidPngHeader() {
        byte[] png = service.generatePng("http://localhost:8080/abc12345");

        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> service.generatePng(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generatePng(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

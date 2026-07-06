package com.urlshortener.service;

import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator(new Base62Encoder());

    @Test
    void generatesCodeOfConfiguredLength() {
        String code = generator.generate("https://example.com/path", 0);

        assertThat(code).hasSize(ShortCodeGenerator.CODE_LENGTH);
    }

    @Test
    void onlyUsesBase62Characters() {
        String code = generator.generate("https://example.com/path", 0);

        assertThat(code).matches("[0-9a-zA-Z]+");
    }

    @Test
    void isDeterministicForSameUrlAndSalt() {
        String first = generator.generate("https://example.com/path", 0);
        String second = generator.generate("https://example.com/path", 0);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void producesDifferentCodesForDifferentSalts() {
        String withoutSalt = generator.generate("https://example.com/path", 0);
        String withSalt = generator.generate("https://example.com/path", 1);

        assertThat(withoutSalt).isNotEqualTo(withSalt);
    }

    @Test
    void producesDifferentCodesForDifferentUrls() {
        String first = generator.generate("https://example.com/path-one", 0);
        String second = generator.generate("https://example.com/path-two", 0);

        assertThat(first).isNotEqualTo(second);
    }
}

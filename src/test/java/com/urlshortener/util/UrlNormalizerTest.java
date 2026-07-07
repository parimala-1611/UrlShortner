package com.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void addsHttpsSchemeWhenMissing() {
        assertThat(normalizer.normalize("example.com/path"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void lowercasesSchemeAndHostButPreservesPathCase() {
        assertThat(normalizer.normalize("HTTPS://Example.COM/Path"))
                .isEqualTo("https://example.com/Path");
    }

    @Test
    void stripsDefaultHttpsPort() {
        assertThat(normalizer.normalize("https://example.com:443/path"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void stripsDefaultHttpPort() {
        assertThat(normalizer.normalize("http://example.com:80/path"))
                .isEqualTo("http://example.com/path");
    }

    @Test
    void keepsNonDefaultPort() {
        assertThat(normalizer.normalize("https://example.com:8443/path"))
                .isEqualTo("https://example.com:8443/path");
    }

    @Test
    void stripsTrailingSlashFromNonRootPath() {
        assertThat(normalizer.normalize("https://example.com/path/"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void treatsRootPathVariantsAsEquivalent() {
        assertThat(normalizer.normalize("https://example.com"))
                .isEqualTo(normalizer.normalize("https://example.com/"));
    }

    @Test
    void stripsFragment() {
        assertThat(normalizer.normalize("https://example.com/page#section"))
                .isEqualTo("https://example.com/page");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(normalizer.normalize("  https://example.com  "))
                .isEqualTo("https://example.com");
    }

    @Test
    void preservesQueryString() {
        assertThat(normalizer.normalize("https://example.com/search?q=test"))
                .isEqualTo("https://example.com/search?q=test");
    }

    @Test
    void rejectsNullUrl() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankUrl() {
        assertThatThrownBy(() -> normalizer.normalize("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThatThrownBy(() -> normalizer.normalize("https:///no-host"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> normalizer.normalize("https://exa mple.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> normalizer.normalize("ftp://example.com/file"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("mailto:test@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("file:///C:/Users/file.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsJumbledTextWithNoDomainStructure() {
        assertThatThrownBy(() -> normalizer.normalize("asdkjhasdkjh"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("randomtext123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFileLikeHosts() {
        assertThatThrownBy(() -> normalizer.normalize("malware.exe"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("document.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("song.mp3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("archive.zip"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsValidDomainsAndIpsAndLocalhost() {
        assertThat(normalizer.normalize("https://www.example.com/path?x=1"))
                .isEqualTo("https://www.example.com/path?x=1");
        assertThat(normalizer.normalize("http://192.168.1.1/page"))
                .isEqualTo("http://192.168.1.1/page");
        assertThat(normalizer.normalize("http://localhost:8080/test"))
                .isEqualTo("http://localhost:8080/test");
    }
}

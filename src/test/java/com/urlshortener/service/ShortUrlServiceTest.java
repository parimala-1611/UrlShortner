package com.urlshortener.service;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.service.exception.ShortCodeGenerationException;
import com.urlshortener.service.exception.ShortUrlExpiredException;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import com.urlshortener.util.UrlNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private UrlNormalizer urlNormalizer;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlService(repository, urlNormalizer, codeGenerator);
    }

    @Test
    void shortenNewUrlGeneratesAndSavesShortUrl() {
        when(urlNormalizer.normalize("raw")).thenReturn("normalized");
        when(repository.findByOriginalUrl("normalized")).thenReturn(Optional.empty());
        when(codeGenerator.generate("normalized", 0)).thenReturn("code1");
        when(repository.findByShortCode("code1")).thenReturn(Optional.empty());
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.shorten("raw", null);

        assertThat(result.getOriginalUrl()).isEqualTo("normalized");
        assertThat(result.getShortCode()).isEqualTo("code1");
    }

    @Test
    void shortenExistingUrlReturnsExistingWithoutGeneratingNewCode() {
        ShortUrl existing = new ShortUrl("normalized", "existingCode", null);
        when(urlNormalizer.normalize("raw")).thenReturn("normalized");
        when(repository.findByOriginalUrl("normalized")).thenReturn(Optional.of(existing));

        ShortUrl result = service.shorten("raw", null);

        assertThat(result).isSameAs(existing);
        verify(codeGenerator, never()).generate(any(), anyInt());
    }

    @Test
    void shortenRetriesWithNextSaltOnCollisionWithDifferentUrl() {
        ShortUrl other = new ShortUrl("other-normalized", "codeA", null);
        when(urlNormalizer.normalize("raw")).thenReturn("normalized");
        when(repository.findByOriginalUrl("normalized")).thenReturn(Optional.empty());
        when(codeGenerator.generate("normalized", 0)).thenReturn("codeA");
        when(codeGenerator.generate("normalized", 1)).thenReturn("codeB");
        when(repository.findByShortCode("codeA")).thenReturn(Optional.of(other));
        when(repository.findByShortCode("codeB")).thenReturn(Optional.empty());
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.shorten("raw", null);

        assertThat(result.getShortCode()).isEqualTo("codeB");
        verify(codeGenerator).generate("normalized", 0);
        verify(codeGenerator).generate("normalized", 1);
    }

    @Test
    void shortenReturnsExistingOnRaceWithSameUrl() {
        ShortUrl raceWinner = new ShortUrl("normalized", "codeA", null);
        when(urlNormalizer.normalize("raw")).thenReturn("normalized");
        when(repository.findByOriginalUrl("normalized")).thenReturn(Optional.empty());
        when(codeGenerator.generate("normalized", 0)).thenReturn("codeA");
        when(repository.findByShortCode("codeA")).thenReturn(Optional.of(raceWinner));

        ShortUrl result = service.shorten("raw", null);

        assertThat(result).isSameAs(raceWinner);
        verify(repository, never()).save(any());
    }

    @Test
    void shortenThrowsAfterExhaustingCollisionAttempts() {
        when(urlNormalizer.normalize("raw")).thenReturn("normalized");
        when(repository.findByOriginalUrl("normalized")).thenReturn(Optional.empty());

        int totalAttempts = ShortUrlService.MAX_COLLISION_ATTEMPTS + 1;
        for (int salt = 0; salt < totalAttempts; salt++) {
            String code = "code" + salt;
            when(codeGenerator.generate("normalized", salt)).thenReturn(code);
            when(repository.findByShortCode(code))
                    .thenReturn(Optional.of(new ShortUrl("other-normalized-" + salt, code, null)));
        }

        assertThatThrownBy(() -> service.shorten("raw", null))
                .isInstanceOf(ShortCodeGenerationException.class);
        verify(codeGenerator, times(totalAttempts)).generate(eq("normalized"), anyInt());
    }

    @Test
    void resolveIncrementsClickCountForNonExpiredCode() {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "code1", null);
        when(repository.findByShortCode("code1")).thenReturn(Optional.of(shortUrl));

        ShortUrl result = service.resolve("code1");

        assertThat(result.getClickCount()).isEqualTo(1L);
    }

    @Test
    void resolveThrowsNotFoundForUnknownCode() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void resolveThrowsExpiredForPastExpiryAndDoesNotIncrementClicks() {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "code1", OffsetDateTime.now().minusSeconds(1));
        when(repository.findByShortCode("code1")).thenReturn(Optional.of(shortUrl));

        assertThatThrownBy(() -> service.resolve("code1"))
                .isInstanceOf(ShortUrlExpiredException.class);
        assertThat(shortUrl.getClickCount()).isZero();
    }

    @Test
    void getStatsReturnsEntityEvenWhenExpired() {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "code1", OffsetDateTime.now().minusSeconds(1));
        when(repository.findByShortCode("code1")).thenReturn(Optional.of(shortUrl));

        ShortUrl result = service.getStats("code1");

        assertThat(result).isSameAs(shortUrl);
    }

    @Test
    void getStatsThrowsNotFoundForUnknownCode() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }
}

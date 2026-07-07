package com.urlshortener.service;

import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlRetentionServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Test
    void purgeExpiredLinksDeletesRowsExpiredBeforeCutoff() {
        ShortUrlRetentionService service = new ShortUrlRetentionService(repository, 90);
        when(repository.deleteByExpiresAtBefore(org.mockito.ArgumentMatchers.any())).thenReturn(3L);

        service.purgeExpiredLinks();

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).deleteByExpiresAtBefore(cutoffCaptor.capture());

        OffsetDateTime expectedCutoff = OffsetDateTime.now().minusDays(90);
        assertThat(cutoffCaptor.getValue()).isBetween(
                expectedCutoff.minusSeconds(5), expectedCutoff.plusSeconds(5));
    }
}

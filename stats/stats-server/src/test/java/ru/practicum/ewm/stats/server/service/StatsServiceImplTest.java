package ru.practicum.ewm.stats.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.server.model.EndpointHit;
import ru.practicum.ewm.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {
    @Mock
    private StatsRepository statsRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsServiceImpl(statsRepository);
    }

    @Test
    void shouldSaveHit() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 22, 12, 0);
        EndpointHitDto hitDto = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.168.0.1",
                timestamp
        );
        ArgumentCaptor<EndpointHit> captor =
                ArgumentCaptor.forClass(EndpointHit.class);

        statsService.saveHit(hitDto);

        verify(statsRepository).save(captor.capture());
        EndpointHit savedHit = captor.getValue();
        assertThat(savedHit.getId()).isNull();
        assertThat(savedHit.getApp()).isEqualTo("ewm-main-service");
        assertThat(savedHit.getUri()).isEqualTo("/events/1");
        assertThat(savedHit.getIp()).isEqualTo("192.168.0.1");
        assertThat(savedHit.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void shouldRejectHitWithEmptyFields() {
        LocalDateTime timestamp = LocalDateTime.now();
        List<EndpointHitDto> invalidHits = List.of(
                new EndpointHitDto(null, "/events", "192.168.0.1", timestamp),
                new EndpointHitDto(" ", "/events", "192.168.0.1", timestamp),
                new EndpointHitDto("ewm-main-service", null,
                        "192.168.0.1", timestamp),
                new EndpointHitDto("ewm-main-service", " ",
                        "192.168.0.1", timestamp),
                new EndpointHitDto("ewm-main-service", "/events",
                        null, timestamp),
                new EndpointHitDto("ewm-main-service", "/events",
                        " ", timestamp),
                new EndpointHitDto("ewm-main-service", "/events",
                        "192.168.0.1", null)
        );

        for (EndpointHitDto invalidHit : invalidHits) {
            assertThatThrownBy(() -> statsService.saveHit(invalidHit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Данные о запросе должны быть заполнены");
        }
    }

    @Test
    void shouldChooseRepositoryMethodByParameters() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 22, 10, 0);
        List<String> uris = List.of("/events");

        statsService.getStats(start, end, null, false);
        statsService.getStats(start, end, uris, false);
        statsService.getStats(start, end, null, true);
        statsService.getStats(start, end, uris, true);

        verify(statsRepository).findStats(start, end);
        verify(statsRepository).findStatsByUris(start, end, uris);
        verify(statsRepository).findUniqueStats(start, end);
        verify(statsRepository).findUniqueStatsByUris(start, end, uris);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 22, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 20, 10, 0);

        assertThatThrownBy(() ->
                statsService.getStats(start, end, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Дата начала диапазона должна быть раньше даты окончания"
                );
    }
}

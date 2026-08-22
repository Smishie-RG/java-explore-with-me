package ru.practicum.ewm.stats.server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StatsRepositoryTest {
    @Autowired
    private StatsRepository statsRepository;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 8, 20, 0, 0);
        end = LocalDateTime.of(2026, 8, 24, 0, 0);
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 22, 12, 0);

        statsRepository.saveAll(List.of(
                createHit("/events", "192.168.0.1", timestamp),
                createHit("/events", "192.168.0.1", timestamp),
                createHit("/events", "192.168.0.2", timestamp),
                createHit("/events/1", "192.168.0.1", timestamp),
                createHit("/events", "192.168.0.3", start.minusDays(1))
        ));
    }

    @Test
    void shouldReturnStatsSortedByHits() {
        List<ViewStats> result = statsRepository.findStats(start, end);

        assertThat(result).containsExactly(
                new ViewStats("ewm-main-service", "/events", 3L),
                new ViewStats("ewm-main-service", "/events/1", 1L)
        );
    }

    @Test
    void shouldFilterStatsByUris() {
        List<ViewStats> result = statsRepository.findStatsByUris(
                start,
                end,
                List.of("/events/1")
        );

        assertThat(result).containsExactly(
                new ViewStats("ewm-main-service", "/events/1", 1L)
        );
    }

    @Test
    void shouldCountUniqueIpAddresses() {
        List<ViewStats> result = statsRepository.findUniqueStats(start, end);

        assertThat(result).containsExactly(
                new ViewStats("ewm-main-service", "/events", 2L),
                new ViewStats("ewm-main-service", "/events/1", 1L)
        );
    }

    @Test
    void shouldCountUniqueIpAddressesForSelectedUris() {
        List<ViewStats> result = statsRepository.findUniqueStatsByUris(
                start,
                end,
                List.of("/events")
        );

        assertThat(result).containsExactly(
                new ViewStats("ewm-main-service", "/events", 2L)
        );
    }

    private EndpointHit createHit(
            String uri,
            String ip,
            LocalDateTime timestamp) {
        return new EndpointHit(
                null,
                "ewm-main-service",
                uri,
                ip,
                timestamp
        );
    }
}

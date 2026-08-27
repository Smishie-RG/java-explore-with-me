package ru.practicum.ewm.main.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventStatsService {
    private static final LocalDateTime STATS_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final String APP = "ewm-main-service";

    private final StatsClient statsClient;

    public void saveHit(HttpServletRequest request) {
        EndpointHitDto hit = new EndpointHitDto(APP, request.getRequestURI(),
                request.getRemoteAddr(), LocalDateTime.now());
        statsClient.saveHit(hit);
    }

    public Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
        List<ViewStats> stats = statsClient.getStats(STATS_START, LocalDateTime.now(), uris, true);
        Map<Long, Long> views = new HashMap<>();
        for (ViewStats stat : stats) {
            String id = stat.getUri().substring(stat.getUri().lastIndexOf('/') + 1);
            views.put(Long.parseLong(id), stat.getHits());
        }
        return views;
    }
}

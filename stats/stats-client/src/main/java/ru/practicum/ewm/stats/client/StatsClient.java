package ru.practicum.ewm.stats.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class StatsClient {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String serverUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public StatsClient(
            @Value("${stats-server.url:http://localhost:9090}") String serverUrl) {
        this(serverUrl, new RestTemplate());
    }

    StatsClient(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }

    public void saveHit(EndpointHitDto hit) {
        restTemplate.postForEntity(serverUrl + "/hit", hit, Void.class);
    }

    public List<ViewStats> getStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(serverUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris);
        }

        URI uri = builder.build().encode().toUri();
        ViewStats[] response = restTemplate.getForObject(uri, ViewStats[].class);

        if (response == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(response);
    }
}
